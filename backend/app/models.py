from sqlmodel import SQLModel, Field, Index
from typing import Optional, Literal
from datetime import datetime

Action = Literal["none", "report", "block", "safe"]

class Event(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)

    # Hashes only (no plaintext numbers, no message body)
    sender_hash: str = Field(index=True, min_length=16, max_length=128)
    device_hash: str = Field(index=True, min_length=16, max_length=128)

    ts: datetime = Field(index=True)
    is_in_contacts: bool = Field(default=False)

    action: Action = Field(default="none")

    __table_args__ = (
        Index("idx_sender_ts", "sender_hash", "ts"),
    )

class Pepper(SQLModel, table=True):
    pepper_id: str = Field(primary_key=True, min_length=4, max_length=32)
    pepper_b64: str = Field(min_length=20, max_length=200)
    created_at: datetime = Field(default_factory=datetime.utcnow, index=True)
    active: bool = Field(default=True, index=True)
