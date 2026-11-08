#!/usr/bin/env python3
"""
توليد صور بـ NVIDIA NIM — FLUX.1-dev
الاستعمال:
    export NVAPI_KEY=nvapi-...
    python3 gen_image.py "برومبت" out.jpg [خطوة] [بذرة]
"""
import os, sys, base64, requests

KEY = os.environ["NVAPI_KEY"]
URL = "https://ai.api.nvidia.com/v1/genai/black-forest-labs/flux.1-dev"

def gen(prompt, out="out.jpg", steps=30, seed=0):
    r = requests.post(URL,
        headers={"Authorization": f"Bearer {KEY}", "Accept": "application/json"},
        json={"text_prompts": [{"text": prompt}], "cfg_scale": 7, "seed": seed, "steps": steps},
        timeout=300)
    r.raise_for_status()
    img = base64.b64decode(r.json()["artifacts"][0]["base64"])
    open(out, "wb").write(img)
    return len(img)

if __name__ == "__main__":
    p = sys.argv[1]
    o = sys.argv[2] if len(sys.argv) > 2 else "out.jpg"
    s = int(sys.argv[3]) if len(sys.argv) > 3 else 30
    sd = int(sys.argv[4]) if len(sys.argv) > 4 else 0
    print(f"✅ {gen(p, o, s, sd)} bytes → {o}")
