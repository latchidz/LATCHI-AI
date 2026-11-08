"""عميل NVIDIA NIM — بث، رؤية، توليد صور."""
from __future__ import annotations
import json, base64, time
import requests
from . import registry as R


class NIMError(Exception):
    def __init__(self, msg, retryable=True, status=None):
        super().__init__(msg)
        self.retryable = retryable
        self.status = status


def _headers(key: str) -> dict:
    return {"Authorization": f"Bearer {key}", "Accept": "application/json",
            "Content-Type": "application/json"}


# ═══════════════════════════════════════════════════════════════
#  بث المحادثة
# ═══════════════════════════════════════════════════════════════
def stream_chat(model: str, messages: list, key: str, *, max_tokens: int = R.MAX_TOKENS,
                temperature: float = R.TEMPERATURE, timeout: int = R.STREAM_TIMEOUT):
    """يولّد قطع نصية. يرفع NIMError إذا الموديل ما خدمش."""
    payload = {"model": model, "messages": messages, "max_tokens": max_tokens,
               "temperature": temperature, "top_p": 0.95, "stream": True}
    try:
        r = requests.post(R.NIM_CHAT_URL, headers=_headers(key), json=payload,
                          stream=True, timeout=(20, timeout))
    except requests.exceptions.Timeout:
        raise NIMError("انتهى الوقت", True)
    except Exception as e:
        raise NIMError(f"{type(e).__name__}", True)

    if r.status_code != 200:
        body = r.text[:200]
        r.close()
        code = r.status_code
        # 404/410/422 ⇒ الموديل ماشي متاح ⇒ نقلبو على واحد آخر
        # 429/500/502/503/504 ⇒ ضغط ⇒ نقلبو
        # 401/403 ⇒ المشكل فـالمفتاح، ماشي فـالموديل
        raise NIMError(f"HTTP {code} · {body}", retryable=(code not in (401,)), status=code)

    r.encoding = "utf-8"          # NVIDIA ما كيعلنش charset ⇒ requests كيغلط
    got_any = False
    try:
        for raw in r.iter_lines(decode_unicode=False):
            if not raw:
                continue
            if isinstance(raw, bytes):
                try: raw = raw.decode("utf-8", "replace")
                except Exception: continue
            if not raw.startswith("data:"):
                continue
            data = raw[5:].strip()
            if data == "[DONE]":
                break
            try:
                j = json.loads(data)
            except Exception:
                continue
            if "error" in j:
                raise NIMError(str(j["error"])[:150], True)
            for ch in j.get("choices", []):
                d = ch.get("delta") or {}
                piece = d.get("content") or ""
                if not piece:
                    # بعض موديلات التفكير كتحط النص فـreasoning — نستعملوه كحل أخير
                    piece = d.get("reasoning_content") or ""
                if piece:
                    got_any = True
                    yield piece
    except NIMError:
        raise
    except Exception as e:
        if got_any:
            return
        raise NIMError(f"بث مقطوع · {type(e).__name__}", True)
    finally:
        try: r.close()
        except Exception: pass

    if not got_any:
        raise NIMError("رد فارغ", True)


# ═══════════════════════════════════════════════════════════════
#  محادثة كاملة (بلا بث)
# ═══════════════════════════════════════════════════════════════
def chat_once(model: str, messages: list, key: str, *, max_tokens: int = R.MAX_TOKENS,
              temperature: float = R.TEMPERATURE, timeout: int = R.REQUEST_TIMEOUT) -> str:
    payload = {"model": model, "messages": messages, "max_tokens": max_tokens,
               "temperature": temperature}
    try:
        r = requests.post(R.NIM_CHAT_URL, headers=_headers(key), json=payload, timeout=timeout)
    except requests.exceptions.Timeout:
        raise NIMError("انتهى الوقت", True)
    except Exception as e:
        raise NIMError(type(e).__name__, True)
    if r.status_code != 200:
        raise NIMError(f"HTTP {r.status_code} · {r.text[:150]}", r.status_code != 401, r.status_code)
    j = r.json()
    m = j["choices"][0]["message"]
    txt = (m.get("content") or m.get("reasoning_content") or "").strip()
    if not txt:
        raise NIMError("رد فارغ", True)
    return txt


# ═══════════════════════════════════════════════════════════════
#  الرؤية — وصف صورة
# ═══════════════════════════════════════════════════════════════
def describe_image(model: str, image_b64: str, mime: str, question: str,
                   key: str, timeout: int = 120) -> str:
    content = [
        {"type": "text", "text": question or "واش كاين فهاد الصورة؟ وصّف بالتفصيل وبالدارجة."},
        {"type": "image_url", "image_url": {"url": f"data:{mime};base64,{image_b64}"}},
    ]
    return chat_once(model, [{"role": "user", "content": content}], key,
                     max_tokens=R.MAX_TOKENS, temperature=0.4, timeout=timeout)


# ═══════════════════════════════════════════════════════════════
#  توليد صور — FLUX.1-dev
# ═══════════════════════════════════════════════════════════════
def gen_image(prompt: str, key: str, *, steps: int = 30, cfg: float = 7.0,
              seed: int = 0, timeout: int = 300) -> bytes:
    body = {"text_prompts": [{"text": prompt}], "cfg_scale": cfg, "seed": seed, "steps": steps}
    try:
        r = requests.post(R.NIM_GENAI_URL + R.IMAGE_MODEL, headers=_headers(key),
                          json=body, timeout=timeout)
    except Exception as e:
        raise NIMError(f"توليد الصورة · {type(e).__name__}", True)
    if r.status_code != 200:
        raise NIMError(f"HTTP {r.status_code} · {r.text[:150]}", True, r.status_code)
    j = r.json()
    arts = j.get("artifacts") or []
    if not arts or not arts[0].get("base64"):
        raise NIMError("بلا صورة", True)
    return base64.b64decode(arts[0]["base64"])


def translate(text: str, key: str, target: str = "English") -> str:
    return chat_once(R.TRANSLATE_MODEL,
                     [{"role": "user", "content": f"Translate to {target}:\n{text}"}],
                     key, max_tokens=R.MAX_TOKENS, timeout=60)
