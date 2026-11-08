#!/usr/bin/env python3
"""
اختبار موديلات NVIDIA NIM — LATCHI AI
الاستعمال:
    export NVAPI_KEY="nvapi-..."
    python3 test_model.py mistralai/mistral-large-4-1t-a52b
"""
import os, sys, json, time, requests

KEY = os.environ.get("NVAPI_KEY", "")
URL = "https://integrate.api.nvidia.com/v1/chat/completions"
MODEL = sys.argv[1] if len(sys.argv) > 1 else "mistralai/mistral-large-4-1t-a52b"

if not KEY:
    sys.exit("❌ حط المفتاح:  export NVAPI_KEY='nvapi-...'")

TESTS = [
    ("دارجة",      [{"role":"system","content":"أنت مساعد جزائري. جاوب بالدارجة الجزائرية فقط."},
                    {"role":"user","content":"واش راك؟ شرحلي بجوج جمل كيفاش ندير فيلم قصير."}], 0.5),
    ("عربية",      [{"role":"user","content":"اشرح في ثلاث نقاط ما هو الذكاء الاصطناعي."}], 0.3),
    ("كود",        [{"role":"user","content":"Python function that reverses a string. Only code."}], 0.2),
]

H = {"Authorization": f"Bearer {KEY}", "Accept": "application/json"}

print(f"\n🔍 الموديل: {MODEL}\n" + "=" * 60)

ok = 0
for name, msgs, temp in TESTS:
    t0 = time.time()
    try:
        r = requests.post(URL, headers=H, timeout=300, json={
            "model": MODEL, "messages": msgs,
            "max_tokens": 2000,          # ⚠️ لازم ≥2000 لموديلات التفكير
            "temperature": temp,
        })
    except Exception as e:
        print(f"\n❌ {name}: {e}"); continue
    dt = time.time() - t0
    if r.status_code != 200:
        print(f"\n❌ {name}: HTTP {r.status_code} — {r.text[:200]}"); continue
    j = r.json()
    m = j["choices"][0]["message"]
    u = j.get("usage", {})
    content = (m.get("content") or "").strip()
    reason = u.get("completion_tokens_details", {}).get("reasoning_tokens", 0)
    print(f"\n{'─'*60}\n{name} · {dt:.1f}s · out={u.get('completion_tokens','?')} · reasoning={reason} · finish={j['choices'][0].get('finish_reason')}")
    print("   " + (content[:500].replace("\n", "\n   ") if content else "⚠️ محتوى فارغ — زيد max_tokens"))
    if content: ok += 1

print(f"\n{'='*60}\n✅ نجح {ok}/{len(TESTS)}")
