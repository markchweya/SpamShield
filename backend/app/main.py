from fastapi import FastAPI, Depends, HTTPException
from sqlmodel import Session, select
from pydantic import BaseModel, Field
from datetime import datetime
import secrets
import base64

from .db import init_db, get_session
from .models import Event, Pepper, Action
from .risk import compute_risk

app = FastAPI(title="SpamShield Backend", version="0.1.0")

@app.on_event("startup")
def on_startup():
    init_db()
    from sqlmodel import Session
    from .db import engine
    with Session(engine) as s:
        active = s.exec(select(Pepper).where(Pepper.active == True)).first()
        if not active:
            pepper_bytes = secrets.token_bytes(32)
            pepper_b64 = base64.b64encode(pepper_bytes).decode("utf-8")
            p = Pepper(pepper_id="p1", pepper_b64=pepper_b64, active=True)
            s.add(p)
            s.commit()

class BootstrapOut(BaseModel):
    pepper_id: str
    pepper_b64: str

@app.get("/v1/bootstrap", response_model=BootstrapOut)
def bootstrap(session: Session = Depends(get_session)):
    p = session.exec(select(Pepper).where(Pepper.active == True)).first()
    if not p:
        raise HTTPException(status_code=500, detail="No active pepper configured")
    return BootstrapOut(pepper_id=p.pepper_id, pepper_b64=p.pepper_b64)

class EventIn(BaseModel):
    sender_hash: str = Field(min_length=16, max_length=128)
    device_hash: str = Field(min_length=16, max_length=128)
    ts_iso: str
    is_in_contacts: bool = False
    action: Action = Action.none

@app.post("/v1/events")
def ingest_event(payload: EventIn, session: Session = Depends(get_session)):
    try:
        ts = datetime.fromisoformat(payload.ts_iso.replace("Z", "+00:00")).replace(tzinfo=None)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid ts_iso (use ISO format)")

    ev = Event(
        sender_hash=payload.sender_hash,
        device_hash=payload.device_hash,
        ts=ts,
        is_in_contacts=payload.is_in_contacts,
        action=payload.action,
    )
    session.add(ev)
    session.commit()
    return {"ok": True}

@app.get("/v1/risk/{sender_hash}")
def risk(sender_hash: str, session: Session = Depends(get_session)):
    return compute_risk(session, sender_hash)

class FeedbackIn(BaseModel):
    sender_hash: str = Field(min_length=16, max_length=128)
    device_hash: str = Field(min_length=16, max_length=128)
    ts_iso: str
    action: Action

@app.post("/v1/feedback")
def feedback(payload: FeedbackIn, session: Session = Depends(get_session)):
    if payload.action not in (Action.report, Action.block, Action.safe):
        raise HTTPException(status_code=400, detail="action must be report|block|safe")

    try:
        ts = datetime.fromisoformat(payload.ts_iso.replace("Z", "+00:00")).replace(tzinfo=None)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid ts_iso")

    ev = Event(
        sender_hash=payload.sender_hash,
        device_hash=payload.device_hash,
        ts=ts,
        is_in_contacts=False,
        action=payload.action,
    )
    session.add(ev)
    session.commit()
    return {"ok": True}

