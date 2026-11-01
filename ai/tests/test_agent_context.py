import base64
import hashlib
import hmac
import json
import time

import pytest

from utils.agent_context import AgentContextError, verify_agent_context

SECRET = "0123456789abcdef0123456789abcdef"


def issue(secret: str = SECRET, expires_offset_ms: int = 60_000) -> str:
    now_ms = int(time.time() * 1000)
    payload = {
        "version": 1,
        "userId": 7,
        "role": "STUDENT",
        "sessionId": 101,
        "traceId": "trace-1",
        "allowedTools": ["queryMyReservations"],
        "issuedAt": now_ms,
        "expiresAt": now_ms + expires_offset_ms,
    }
    encoded = _encode_urlsafe(
        json.dumps(
            payload,
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode("utf-8")
    )
    signature = hmac.new(
        secret.encode("utf-8"),
        encoded.encode("ascii"),
        hashlib.sha256,
    ).digest()
    return f"{encoded}.{_encode_urlsafe(signature)}"


def test_verify_context() -> None:
    claims = verify_agent_context(issue(), SECRET)

    assert claims.user_id == 7
    assert claims.role == "STUDENT"
    assert claims.session_id == 101
    assert claims.allowed_tools == ["queryMyReservations"]


def test_reject_tampered_context() -> None:
    token = issue()
    tampered = token[:-1] + ("A" if token[-1] != "A" else "B")

    with pytest.raises(AgentContextError):
        verify_agent_context(tampered, SECRET)


def test_reject_expired_context() -> None:
    with pytest.raises(AgentContextError):
        verify_agent_context(issue(expires_offset_ms=-1), SECRET)


def test_reject_missing_secret() -> None:
    with pytest.raises(AgentContextError):
        verify_agent_context(issue(), "")


def test_reject_mismatched_request_id() -> None:
    with pytest.raises(AgentContextError):
        verify_agent_context(issue(), SECRET, "trace-2")


def _encode_urlsafe(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")
