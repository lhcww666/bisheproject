import base64
import hashlib
import hmac
import time
from typing import Final

from schemas.agent import AgentContextClaims

CONTEXT_VERSION: Final[int] = 1
MAX_TOKEN_LENGTH: Final[int] = 8192
ROLES: Final[set[str]] = {"STUDENT", "TEACHER", "ADMIN", "USER"}


class AgentContextError(ValueError):
    """Agent 服务上下文无效。"""


def verify_agent_context(token: str, secret: str) -> AgentContextClaims:
    if not secret or len(secret.encode("utf-8")) < 32:
        raise AgentContextError("Agent 服务上下文密钥未配置或长度不足32字节")
    if not token or len(token) > MAX_TOKEN_LENGTH:
        raise AgentContextError("Agent 服务上下文无效或已过期")

    parts = token.split(".")
    if len(parts) != 2 or not parts[0] or not parts[1]:
        raise AgentContextError("Agent 服务上下文无效或已过期")

    expected = hmac.new(
        secret.encode("utf-8"),
        parts[0].encode("ascii"),
        hashlib.sha256,
    ).digest()
    try:
        actual = _decode_urlsafe(parts[1])
    except ValueError as exception:
        raise AgentContextError("Agent 服务上下文无效或已过期") from exception
    if not hmac.compare_digest(actual, expected):
        raise AgentContextError("Agent 服务上下文无效或已过期")

    try:
        payload = _decode_urlsafe(parts[0])
        claims = AgentContextClaims.model_validate_json(payload)
    except Exception as exception:
        raise AgentContextError("Agent 服务上下文无效或已过期") from exception

    now_ms = int(time.time() * 1000)
    if (
        claims.version != CONTEXT_VERSION
        or claims.user_id <= 0
        or claims.session_id <= 0
        or not claims.trace_id
        or claims.role not in ROLES
        or claims.expires_at <= now_ms
        or claims.expires_at <= claims.issued_at
        or claims.issued_at > now_ms + 60_000
    ):
        raise AgentContextError("Agent 服务上下文无效或已过期")
    return claims


def _decode_urlsafe(value: str) -> bytes:
    padding = "=" * (-len(value) % 4)
    return base64.urlsafe_b64decode(value + padding)
