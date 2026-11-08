"""
صحة الموديلات — عقوبات تدريجية ورجوع تلقائي.
موديل يطيح ⇒ يخرج من الدورة مؤقتاً ⇒ يرجع وحده من بعد.
"""
from __future__ import annotations
import threading, time
from . import registry as R


class Health:
    def __init__(self):
        self._lock = threading.Lock()
        self._state: dict[str, dict] = {}

    def _s(self, model: str) -> dict:
        return self._state.setdefault(model, {
            "fails": 0, "cooldown_until": 0.0,
            "ok": 0, "total": 0, "last_ok": 0.0, "last_fail": 0.0,
            "latency": [], "reason": "",
        })

    # ── تسجيل ───────────────────────────────────────────
    def mark_ok(self, model: str, latency: float):
        with self._lock:
            s = self._s(model)
            s["ok"] += 1; s["total"] += 1
            s["fails"] = 0
            s["cooldown_until"] = 0.0
            s["last_ok"] = time.time()
            s["reason"] = ""
            s["latency"] = (s["latency"] + [latency])[-8:]

    def mark_fail(self, model: str, reason: str = ""):
        with self._lock:
            s = self._s(model)
            s["fails"] += 1; s["total"] += 1
            s["last_fail"] = time.time()
            s["reason"] = (reason or "")[:120]
            n = max(0, s["fails"] - 1)
            cd = min(R.COOLDOWN_BASE * (2 ** n), R.COOLDOWN_MAX)
            s["cooldown_until"] = time.time() + cd

    # ── قراءة ───────────────────────────────────────────
    def available(self, model: str) -> bool:
        s = self._state.get(model)
        return not s or s["cooldown_until"] <= time.time()

    def avg_latency(self, model: str) -> float:
        s = self._state.get(model)
        if not s or not s["latency"]:
            return 99.0
        return sum(s["latency"]) / len(s["latency"])

    def chain(self, models: list[str], limit: int | None = None) -> list[str]:
        """يرتّب السلسلة: الشغالين أولاً، الأسرع أولاً، والتعبانين آخراً."""
        now = time.time()
        alive, dead = [], []
        for m in models:
            s = self._state.get(m, {})
            (dead if s.get("cooldown_until", 0) > now else alive).append(m)
        alive.sort(key=lambda m: (self.avg_latency(m), self._state.get(m, {}).get("fails", 0)))
        out = alive + dead
        return out[:limit] if limit else out

    def snapshot(self) -> dict:
        now = time.time()
        out = {}
        for m, s in self._state.items():
            out[m] = {
                "ok": s["ok"], "total": s["total"],
                "fails": s["fails"],
                "cooling": s["cooldown_until"] > now,
                "cooldown_left": max(0, int(s["cooldown_until"] - now)),
                "latency": round(self.avg_latency(m), 2) if s["latency"] else None,
                "reason": s["reason"],
            }
        return out


health = Health()
