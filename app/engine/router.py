"""
المحرّك الذكي — يختار الموديل، يبدّل تلقائياً كي يطيح، ويكمّل نفس المحادثة.

المبدأ:
  كل موديل عندو صحة. المحرّك كيبني «سلسلة» مرتبة بالسرعة والنجاح.
  أول واحد ما يخدمش ⇒ يخرج من الدورة ⇒ الثاني يدخل ⇒ نفس الرسائل بالضبط.
  المستخدم ما كيحسّش بوالو، غير يشوف «⚡ تبديل تلقائي».
"""
from __future__ import annotations
import time, itertools, threading, base64
from . import registry as R
from . import nim
from .health import health

_key_lock = threading.Lock()
_key_rr = itertools.count()


def pick_key(offset: int = 0) -> str:
    keys = R.api_keys()
    if not keys:
        raise RuntimeError("ماكاينش مفتاح NVAPI")
    with _key_lock:
        i = (next(_key_rr) + offset) % len(keys)
    return keys[i]


def key_count() -> int:
    try: return len(R.api_keys())
    except Exception: return 0


# ═══════════════════════════════════════════════════════════════
#  البث مع التبديل التلقائي
# ═══════════════════════════════════════════════════════════════
def stream_with_failover(models: list[str], messages: list, *, temperature: float = R.TEMPERATURE,
                         max_tokens: int = R.MAX_TOKENS, attempts: int = R.MAX_ATTEMPTS):
    """
    يرجّع generator فيه dicts:
      {"type":"model","model":...}   — بدا الموديل
      {"type":"switch","from":...,"to":...,"reason":...}
      {"type":"delta","text":...}
      {"type":"reset"}               — امسح الجزء اللي تكتب (تبديل وسط البث)
      {"type":"done","model":...,"elapsed":...}
      {"type":"error","message":...}
    """
    chain = health.chain(models, limit=attempts)
    tried, emitted_any, last_err = [], False, ""

    for idx, model in enumerate(chain):
        key = pick_key(idx)
        t0 = time.time()
        if idx == 0:
            yield {"type": "model", "model": model, "auto": True}
        else:
            yield {"type": "switch", "from": tried[-1] if tried else "", "to": model,
                   "reason": last_err[:90]}

        got = False
        try:
            for piece in nim.stream_chat(model, messages, key,
                                         max_tokens=max_tokens, temperature=temperature):
                if not got:
                    got = True
                    emitted_any = True
                yield {"type": "delta", "text": piece}
            if got:
                health.mark_ok(model, time.time() - t0)
                yield {"type": "done", "model": model, "elapsed": round(time.time() - t0, 1)}
                return
            raise nim.NIMError("رد فارغ", True)
        except nim.NIMError as e:
            last_err = str(e)
            health.mark_fail(model, last_err)
            tried.append(model)
            if got:
                yield {"type": "reset"}
            if not e.retryable:
                yield {"type": "error", "message": "مشكل فـالمفتاح، ماشي فـالموديل"}
                return
            continue

    yield {"type": "error",
           "message": "كل الموديلات تعبوا حالياً. عاود من بعد شوية."}


def complete_with_failover(models: list[str], messages: list, *, temperature=R.TEMPERATURE,
                           max_tokens=R.MAX_TOKENS, attempts=R.MAX_ATTEMPTS) -> tuple[str, str]:
    """نسخة بلا بث — ترجّع (النص، الموديل اللي خدم)."""
    chain = health.chain(models, limit=attempts)
    last = ""
    for idx, model in enumerate(chain):
        t0 = time.time()
        try:
            txt = nim.chat_once(model, messages, pick_key(idx),
                                max_tokens=max_tokens, temperature=temperature)
            health.mark_ok(model, time.time() - t0)
            return txt, model
        except nim.NIMError as e:
            last = str(e)
            health.mark_fail(model, last)
    raise RuntimeError(last or "كل الموديلات طاحت")



# ═══════════════════════════════════════════════════════════════
#  نحوّل طلب المستخدم لبرومبت إنجليزي احترافي (فلوكس ما كيفهمش العربية)
# ═══════════════════════════════════════════════════════════════
IMAGE_PROMPT_SYS = (
    "You convert image requests into ONE detailed English image-generation prompt. "
    "Output ONLY the prompt, no quotes, no explanation. "
    "Include: subject, setting, lighting, mood, camera/lens, style, quality keywords. "
    "Max 60 words."
)

def build_image_prompt(user_text: str) -> str:
    base = R.clean_image_prompt(user_text)
    try:
        en, _ = complete_with_failover(
            R.TEXT_CHAIN[:2],
            [{"role": "system", "content": IMAGE_PROMPT_SYS},
             {"role": "user", "content": base}],
            temperature=0.4, max_tokens=R.MAX_TOKENS, attempts=2)
        en = en.strip().strip('"').strip()
        if 8 < len(en) < 400:
            return en
    except Exception:
        pass
    return base


# ═══════════════════════════════════════════════════════════════
#  خط الأنابيب الكامل — كل الموديلات كتخدم مع بعضها
# ═══════════════════════════════════════════════════════════════
def build_messages(history: list[dict], system: str = R.SYSTEM_PROMPT) -> list[dict]:
    msgs = [{"role": "system", "content": system}]
    for m in history[-R.MAX_HISTORY_TURNS:]:
        role = m.get("role")
        if role not in ("user", "assistant"):
            continue
        content = m.get("content") or ""
        if content.strip():
            msgs.append({"role": role, "content": content})
    return msgs


def respond(history: list[dict], user_text: str, images: list[dict] | None = None):
    """
    يرجّع generator موحّد لكل الحالات:
      · طلب رسم   ⇒ FLUX يولّد صورة
      · فيه صورة  ⇒ موديل رؤية يوصّف ← موديل نص يجاوب
      · نص فقط    ⇒ سلسلة النص مباشرة
    """
    images = images or []

    # ── 1. طلب توليد صورة ──────────────────────────────
    if R.wants_image(user_text) and not images:
        yield {"type": "route", "route": "image"}
        yield {"type": "status", "text": "كنحوّل الوصف..."}
        prompt = build_image_prompt(user_text)
        yield {"type": "status", "text": "كنولّد الصورة..."}
        try:
            png = nim.gen_image(prompt, pick_key(), steps=30)
            b64 = base64.b64encode(png).decode()
            yield {"type": "image", "b64": b64, "mime": "image/jpeg", "prompt": prompt}
            yield {"type": "done", "model": R.IMAGE_MODEL, "elapsed": 0}
            return
        except nim.NIMError as e:
            yield {"type": "status", "text": "توليد الصورة تعطّل — نكمّل بالنص..."}
            user_text = (f"(توليد الصورة تعطّل: {e}) " + user_text)

    # ── 2. صور مرفقة ⇒ وصف من موديل رؤية ───────────────
    vision_note = ""
    if images:
        yield {"type": "route", "route": "vision"}
        yield {"type": "status", "text": "كنقرا الصورة..."}
        chain = health.chain(R.VISION_CHAIN, limit=3)
        q = user_text or "وصّف هاد الصورة بالتفصيل."
        for i, vm in enumerate(chain):
            try:
                t0 = time.time()
                vision_note = nim.describe_image(
                    vm, images[0]["b64"], images[0].get("mime", "image/jpeg"), q, pick_key(i))
                health.mark_ok(vm, time.time() - t0)
                yield {"type": "vision_used", "model": vm}
                break
            except nim.NIMError as e:
                health.mark_fail(vm, str(e))
                continue

    # ── 3. الصياغة النهائية على سلسلة النص ────────────
    msgs = build_messages(history)
    if vision_note:
        msgs.append({"role": "system",
                     "content": "المستخدم رفع صورة، وهذا وصفها من موديل الرؤية:\n" + vision_note})
    msgs.append({"role": "user", "content": user_text or "وصّف الصورة."})

    yield {"type": "route", "route": "text"}
    yield from stream_with_failover(R.TEXT_CHAIN, msgs)
