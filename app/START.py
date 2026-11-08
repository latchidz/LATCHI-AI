#!/usr/bin/env python3
"""
LATCHI AI — مشغّل بضغطة واحدة
يفحص المتطلبات، يثبّت الناقص، ويطلق المتصفح.

    python3 START.py
"""
import os, sys, subprocess, time, webbrowser, socket
from pathlib import Path

HERE = Path(__file__).resolve().parent
os.chdir(HERE)
PORT = int(os.environ.get("PORT", 8000))

C = {"g": "\033[92m", "y": "\033[93m", "r": "\033[91m", "b": "\033[94m", "0": "\033[0m"}
def say(k, m): print(f"{C[k]}{m}{C['0']}")

def check_python():
    if sys.version_info < (3, 10):
        say("r", f"✗ تحتاج Python 3.10+ — عندك {sys.version.split()[0]}")
        sys.exit(1)
    say("g", f"✓ Python {sys.version.split()[0]}")

def pip_install(pkgs):
    for p in pkgs:
        say("y", f"… كنثبّت {p}")
        subprocess.run([sys.executable, "-m", "pip", "install", "-q", p],
                       check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def check_deps():
    need = []
    for mod, pkg in [("fastapi", "fastapi"), ("uvicorn", "uvicorn"),
                     ("requests", "requests"), ("multipart", "python-multipart")]:
        try:
            __import__(mod)
            say("g", f"✓ {pkg}")
        except ImportError:
            need.append(pkg)
    if need:
        pip_install(need)
    for mod in ["fastapi", "uvicorn", "requests", "multipart"]:
        try: __import__(mod)
        except ImportError:
            say("r", f"✗ {mod} ما تثبّطش"); sys.exit(1)

def check_env():
    env = HERE / ".env"
    if not env.exists():
        ex = HERE / ".env.example"
        if ex.exists():
            env.write_text(ex.read_text(encoding="utf-8"), encoding="utf-8")
        say("r", "✗ ماكاينش app/.env")
        say("y", "  افتح app/.env وحطّ: NVAPI_KEYS=nvapi-xxxxx")
        say("y", "  من بعد عاود شغّل: python3 START.py")
        sys.exit(1)
    txt = env.read_text(encoding="utf-8")
    if "nvapi-xxxxx" in txt or "NVAPI_KEYS=" not in txt:
        say("r", "✗ المفتاح ما تحطّش فـapp/.env")
        sys.exit(1)
    say("g", "✓ المفتاح موجود")

def free_port(p):
    with socket.socket() as s:
        return s.connect_ex(("127.0.0.1", p)) != 0

def main():
    print("\n" + "═" * 52)
    say("b", "   LATCHI AI  ·  v2.0")
    print("═" * 52 + "\n")
    check_python(); check_deps(); check_env()

    port = PORT
    while not free_port(port):
        port += 1
    if port != PORT:
        say("y", f"⚠ البورت {PORT} مشغول — كنستعمل {port}")

    url = f"http://127.0.0.1:{port}"
    print()
    say("g", f"🚀 كنطلق على {url}")
    say("y", "   (باش تسدّ: Ctrl+C)\n")

    def open_later():
        time.sleep(2.5)
        try: webbrowser.open(url)
        except Exception: pass
    import threading
    threading.Thread(target=open_later, daemon=True).start()

    os.environ["PORT"] = str(port)
    os.execv(sys.executable, [sys.executable, "-m", "uvicorn", "main:app",
                              "--host", "127.0.0.1", "--port", str(port),
                              "--log-level", "warning"])

if __name__ == "__main__":
    main()
