"""
LATCHI AI · الخادم الرئيسي
كل شي مدمج: المحرّك، المفاتيح، الموديلات، الذاكرة — الواجهة ما تشوف حتى إعداد.
"""
from __future__ import annotations
import json, os, time, uuid, base64, asyncio
from pathlib import Path
from fastapi import FastAPI, Request, UploadFile, File, Form, HTTPException
from fastapi.responses import StreamingResponse, JSONResponse, FileResponse, HTMLResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import threading

from engine import registry as R
from engine import nim
from engine.router import respond, stream_with_failover, key_count, pick_key
from engine.health import health

BASE = Path(__file__).parent
STATIC = BASE / "static"
DATA = BASE / "data"
DATA.mkdir(exist_ok=True)

APP_VERSION = "2.0.0"
app = FastAPI(title="LATCHI AI", version=APP_VERSION, docs_url=None, redoc_url=None)

_store_lock = threading.Lock()
_store_path = DATA / "sessions.json"


def _load_store() -> dict:
    try:
        return json.loads(_store_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def _save_store(store: dict):
    try:
        tmp = _store_path.with_suffix(".tmp")
        tmp.write_text(json.dumps(store, ensure_ascii=False), encoding="utf-8")
        tmp.replace(_store_path)
    except Exception:
        pass


SESSIONS: dict = _load_store()


# ═══════════════════════════════════════════════════════════════
#  عارض الحالة
# ═══════════════════════════════════════════════════════════════
@app.get("/api/status")
def api_status():
    snap = health.snapshot()
    alive = [m for m in R.TEXT_CHAIN if not snap.get(m, {}).get("cooling", False)]
    return {
        "version": APP_VERSION,
        "ready": True,
        "models_total": len(R.TEXT_CHAIN),
        "models_alive": len(alive),
        "keys": key_count(),
        "image_model": R.IMAGE_MODEL,
        "image_ready": True,
    }


# ═══════════════════════════════════════════════════════════════
#  المحادثات
# ═══════════════════════════════════════════════════════════════
class NewChat(BaseModel):
    title: str | None = None


@app.get("/api/chats")
def list_chats():
    out = []
    for cid, s in SESSIONS.items():
        msgs = s.get("messages", [])
        out.append({
            "id": cid,
            "title": s.get("title") or "محادثة جديدة",
            "updated": s.get("updated", 0),
            "count": len(msgs),
        })
    out.sort(key=lambda x: x["updated"], reverse=True)
    return out


@app.post("/api/chats")
def create_chat(body: NewChat):
    cid = uuid.uuid4().hex[:12]
    with _store_lock:
        SESSIONS[cid] = {"title": body.title or "محادثة جديدة",
                         "messages": [], "updated": time.time()}
        _save_store(SESSIONS)
    return {"id": cid}


@app.get("/api/chats/{cid}")
def get_chat(cid: str):
    s = SESSIONS.get(cid)
    if not s:
        raise HTTPException(404, "ماكاينش")
    return {"id": cid, "title": s["title"], "messages": s["messages"]}


@app.delete("/api/chats/{cid}")
def delete_chat(cid: str):
    with _store_lock:
        SESSIONS.pop(cid, None)
        _save_store(SESSIONS)
    return {"ok": True}


# ═══════════════════════════════════════════════════════════════
#  الشات — بث SSE مع تبديل الموديل التلقائي
# ═══════════════════════════════════════════════════════════════
class ChatIn(BaseModel):
    chat_id: str
    text: str
    images: list[dict] = []


def sse(obj: dict) -> str:
    return f"data: {json.dumps(obj, ensure_ascii=False)}\n\n"


@app.post("/api/chat")
async def api_chat(body: ChatIn):
    s = SESSIONS.get(body.chat_id)
    if not s:
        raise HTTPException(404, "المحادثة ماكانتش")

    history = list(s.get("messages", []))
    text = (body.text or "").strip()
    if not text and not body.images:
        raise HTTPException(400, "رسالة خاوية")

    # نسجّل رسالة المستخدم
    s.setdefault("messages", []).append({"role": "user", "content": text})
    if s["title"] in ("محادثة جديدة", "") and text:
        s["title"] = text[:40]
    s["updated"] = time.time()

    def gen():
        answer_parts = []
        model_used = ""
        produced_image = None
        try:
            for ev in respond(history, text, body.images):
                t = ev.get("type")
                if t == "delta":
                    answer_parts.append(ev["text"])
                    yield sse(ev)
                elif t == "reset":
                    answer_parts.clear()
                    yield sse(ev)
                elif t == "done":
                    model_used = ev.get("model", "")
                    yield sse(ev)
                elif t == "image":
                    produced_image = ev.get("b64")
                    answer_parts.append(f"[صورة مولّدة: {ev.get('prompt','')}]")
                    yield sse(ev)
                else:
                    yield sse(ev)
        except Exception as e:
            yield sse({"type": "error", "message": f"{type(e).__name__}: {e}"})
        finally:
            final = "".join(answer_parts).strip()
            if final:
                with _store_lock:
                    SESSIONS[body.chat_id].setdefault("messages", []).append({
                        "role": "assistant", "content": final,
                        "model": model_used, "image": produced_image,
                        "ts": time.time(),
                    })
                    SESSIONS[body.chat_id]["updated"] = time.time()
                    _save_store(SESSIONS)
            yield sse({"type": "end"})

    return StreamingResponse(gen(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache",
                                      "X-Accel-Buffering": "no",
                                      "Connection": "keep-alive"})


# ═══════════════════════════════════════════════════════════════
#  رفع صورة
# ═══════════════════════════════════════════════════════════════
@app.post("/api/upload")
async def upload(file: UploadFile = File(...)):
    raw = await file.read()
    if len(raw) > 12 * 1024 * 1024:
        raise HTTPException(413, "الصورة كبيرة بزاف (12MB حد أقصى)")
    mime = file.content_type or "image/jpeg"
    if not mime.startswith("image/"):
        raise HTTPException(415, "غير الصور مسموحة")
    return {"b64": base64.b64encode(raw).decode(), "mime": mime,
            "name": file.filename, "size": len(raw)}


# ═══════════════════════════════════════════════════════════════
#  توليد صورة مباشر
# ═══════════════════════════════════════════════════════════════
class ImgIn(BaseModel):
    prompt: str
    steps: int = 30


@app.post("/api/image")
def api_image(body: ImgIn):
    try:
        png = nim.gen_image(body.prompt, pick_key(), steps=max(4, min(body.steps, 50)))
        return {"b64": base64.b64encode(png).decode(), "mime": "image/jpeg"}
    except nim.NIMError as e:
        raise HTTPException(502, str(e))


# ═══════════════════════════════════════════════════════════════
#  الواجهة
# ═══════════════════════════════════════════════════════════════
app.mount("/static", StaticFiles(directory=str(STATIC)), name="static")


@app.get("/", response_class=HTMLResponse)
def index():
    return (STATIC / "index.html").read_text(encoding="utf-8")


@app.get("/health")
def healthz():
    return {"ok": True, "v": APP_VERSION}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", 8000)), log_level="info")
