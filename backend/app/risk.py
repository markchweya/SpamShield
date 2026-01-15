from datetime import datetime, timedelta
from sqlmodel import Session, select
from .models import Event
import math

def _log1p(x: int) -> float:
    return math.log(1 + max(0, x))

def compute_risk(session: Session, sender_hash: str, now: datetime | None = None) -> dict:
    now = now or datetime.utcnow()
    t24 = now - timedelta(hours=24)
    t7d = now - timedelta(days=7)

    ev_24 = session.exec(
        select(Event).where(Event.sender_hash == sender_hash, Event.ts >= t24)
    ).all()

    uniq_devices_24h = len({e.device_hash for e in ev_24})
    reports_24h = sum(1 for e in ev_24 if e.action == "report")
    blocks_24h = sum(1 for e in ev_24 if e.action == "block")

    ev_7d = session.exec(
        select(Event.is_in_contacts).where(Event.sender_hash == sender_hash, Event.ts >= t7d)
    ).all()
    if ev_7d:
        in_contacts_rate_7d = sum(1 for (v,) in ev_7d if v) / len(ev_7d)
    else:
        in_contacts_rate_7d = 0.0

    score = (
        20 * _log1p(uniq_devices_24h)
        + 18 * _log1p(reports_24h + blocks_24h)
        - 35 * in_contacts_rate_7d
    )
    score = max(0, min(100, int(round(score))))

    reasons: list[str] = []
    if uniq_devices_24h >= 10:
        reasons.append(f"High reach: seen on {uniq_devices_24h} distinct devices in 24h")
    elif uniq_devices_24h >= 4:
        reasons.append(f"Moderate reach: seen on {uniq_devices_24h} devices in 24h")

    if (reports_24h + blocks_24h) >= 3:
        reasons.append(f"Many negative actions: {reports_24h} reports + {blocks_24h} blocks (24h)")

    if in_contacts_rate_7d <= 0.05 and len(ev_7d) >= 10:
        reasons.append("Rarely saved in contacts across recipients (7d)")

    level = "normal"
    if score >= 80:
        level = "high"
    elif score >= 50:
        level = "medium"

    return {
        "sender_hash": sender_hash,
        "score": score,
        "level": level,
        "signals": {
            "uniq_devices_24h": uniq_devices_24h,
            "reports_24h": reports_24h,
            "blocks_24h": blocks_24h,
            "in_contacts_rate_7d": round(in_contacts_rate_7d, 4),
        },
        "reasons": reasons[:4],
    }
