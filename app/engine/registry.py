"""
LATCHI AI — سجل الموديلات (سيرفر فقط · مخفي تماماً عن الواجهة)
كل الإعدادات هنا. المستخدم ما يشوف حتى شي.
"""
from __future__ import annotations
import json, os, re

# ═══════════════════════════════════════════════════════════════
#  NVIDIA NIM
# ═══════════════════════════════════════════════════════════════
NIM_CHAT_URL = "https://integrate.api.nvidia.com/v1/chat/completions"
NIM_MODELS_URL = "https://integrate.api.nvidia.com/v1/models"
NIM_GENAI_URL = "https://ai.api.nvidia.com/v1/genai/"

# ── سلاسل النص — مرتبة بالأولوية (الأسرع والأنظف فـالدارجة أولاً) ──
# المحرّك كيدور عليهم بالترتيب، وأول واحد يخدم هو اللي كيجاوب.
TEXT_CHAIN = [
    "nvidia/nemotron-3.5-bolt-e4b",            #  0.3s · دارجة مزيانة
    "nvidia/nemotron-3-ultra-550b-a55b",       #  2.1s · أحسن دارجة
    "nvidia/nemotron-3.5-super-vl-120b-a12b",  #  0.5s · بصري
    "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",  # 3.5s
    "minimaxai/minimax-m3.1-flash",            #  1.5s
    "meta/muse-glimmer-30b",                   #  6.1s
    "nvidia/nemotron-3.5-lightning-30b-a3b",   # 10.6s
    "stepfun-ai/step-5-preview",               # 11.3s · بصري
    "mistralai/mistral-large-4-1t-a52b",       # 13.7s · tools
    "poolside/laguna-xs-2.1",                  #  0.2s · احتياط سريع
    "meta/llama-3.2-90b-vision-instruct",      # 68.7s · ثقيل، آخر حل
]

# ── سلاسل الصورة (vision) — كيوصف الصورة قبل ما يجاوب النص ──
VISION_CHAIN = [
    "nvidia/nemotron-3.5-super-vl-120b-a12b",          # 0.5s
    "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",   # 3.5s
    "stepfun-ai/step-5-preview",                       # 11s
    "meta/llama-3.2-90b-vision-instruct",              # 68s
]

# ── توليد الصور ──
IMAGE_MODEL = "black-forest-labs/flux.1-dev"
IMAGE_FALLBACK = ["black-forest-labs/flux.1-schnell"]

# ── ترجمة ──
TRANSLATE_MODEL = "nvidia/riva-translate-4b-instruct-v2"

# ── معايير التشغيل ──
MAX_TOKENS        = 2000     # أقل من هذا ⇒ رد فارغ مع موديلات التفكير
TEMPERATURE       = 0.7
REQUEST_TIMEOUT   = 180      # ثانية
STREAM_TIMEOUT    = 180
MAX_HISTORY_TURNS = 24       # قداش من رسالة تبقى فـالذاكرة
MAX_ATTEMPTS      = 4        # قداش موديل نجرّب قبل ما نستسلم
COOLDOWN_BASE     = 90       # ثانية — أول عقوبة
COOLDOWN_MAX      = 900      # سقف العقوبة
FAIL_THRESHOLD    = 1        # فشل واحد ⇒ خروج من الدورة مؤقتاً

# ═══════════════════════════════════════════════════════════════
#  شخصية المساعد — مدمجة، ما تتبدّلش من الواجهة
# ═══════════════════════════════════════════════════════════════
SYSTEM_PROMPT = """أنت «لاشي أي آي» (Latchi AI)، مساعد ذكي جزائري تابع لمشروع لاشي.

قواعد اللغة:
- ردّ دائماً بالدارجة الجزائرية المفهومة، مكتوبة بحروف عربية واضحة.
- إذا كتب المستخدم بالعربية الفصحى أو الإنجليزية، ردّ بنفس لغته.
- المصطلحات التقنية والأسماء الأجنبية خلّيها كيف راهي بلا ترجمة.

أسلوبك:
- مباشر وواضح، بلا حشو وبلا مبالغة.
- إذا السؤال قصير جاوب قصير، وإذا يحتاج تفصيل فصّل.
- استعمل التنسيق (نقاط، عناوين، كود) كي يساعد على القراءة.
- إذا ما عرفتش، قول «ما نعرفش» بكل صراحة.

الكود:
- كتب كود كامل وقابل للتشغيل، وشرح الزائد خلّيه برّا.
- استعمل ``` مع اسم اللغة."""


# ═══════════════════════════════════════════════════════════════
#  كشف النية — بلا ما نستنّاو موديل
# ═══════════════════════════════════════════════════════════════
IMAGE_INTENT = [
    "ارسم", "رسم", "صمم", "صوّر", "ولّد صورة", "ولد صورة", "صنع صورة",
    "اصنع صورة", "أعطني صورة", "عطيني صورة", "صورة لـ", "صورة على",
    "draw", "generate an image", "create an image", "make an image",
    "paint", "illustrate", "image of", "picture of", "flux",
]

CODE_INTENT = ["كود", "برنامج", "function", "class ", "script", "debug", "خطأ فـالكود",
               "code", "python", "javascript", "kotlin", "html", "css", "sql"]


def wants_image(text: str) -> bool:
    t = (text or "").lower().strip()
    if len(t) > 400:          # نص طويل ⇒ ماشي طلب رسم
        return False
    hits = sum(1 for k in IMAGE_INTENT if k in t)
    # لازم كلمة صريحة + ماشي سؤال عن الصور
    neg = ["واش رايك فـالصورة", "شنو فيها الصورة", "حلل الصورة", "describe"]
    if any(n in t for n in neg):
        return False
    return hits >= 1 and any(k in t for k in IMAGE_INTENT[:13] + IMAGE_INTENT[13:])


def clean_image_prompt(text: str) -> str:
    """يحذف كلمة الطلب من البداية بلا ما يقطع الكلمة."""
    t = text.strip()
    low = t.lower()
    for k in sorted(IMAGE_INTENT, key=len, reverse=True):
        if low.startswith(k):
            rest = t[len(k):]
            # نشيلو اللواحق الموصولة: لي · ني · لنا · ي · نا
            rest = re.sub(r"^\s*(لي|ني|لنا|نا|ي|و|ال)?\s*", "", rest)
            t = rest or t
            break
    return t.strip(" :،.-") or text


def load_env():
    """يقرا المفاتيح من .env ولا من متغيرات البيئة."""
    env = {}
    path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")
    if os.path.exists(path):
        for line in open(path, encoding="utf-8"):
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def api_keys() -> list[str]:
    """كل المفاتيح المتوفرة — المحرّك كيبدّل بيناتهم إذا واحد طاح."""
    e = load_env()
    keys = []
    for raw in (e.get("NVAPI_KEYS") or e.get("NVAPI_KEY") or "").split(","):
        raw = raw.strip()
        if raw.startswith("nvapi-"):
            keys.append(raw)
    if not keys:
        k = os.environ.get("NVAPI_KEY", "").strip()
        if k:
            keys.append(k)
    # مفاتيح احتياطية مدمجة (تتقرا من .env فقط — ما تنكتبش هنا)
    extra = e.get("NVAPI_EXTRA", "")
    for raw in extra.split(","):
        raw = raw.strip()
        if raw.startswith("nvapi-") and raw not in keys:
            keys.append(raw)
    return keys
