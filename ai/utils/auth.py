from typing import Annotated

from fastapi import Header, HTTPException

from config.settings import get_settings
from schemas.agent import AgentAuthContext, AgentContextClaims
from utils.agent_context import AgentContextError, verify_agent_context


def require_agent_context(
    x_agent_context: Annotated[
        str | None,
        Header(alias="X-Agent-Context"),
    ] = None,
    x_request_id: Annotated[
        str | None,
        Header(alias="X-Request-Id"),
    ] = None,
) -> AgentAuthContext:
    try:
        claims: AgentContextClaims = verify_agent_context(
            x_agent_context or "",
            get_settings().agent_context_secret,
            x_request_id,
        )
        return AgentAuthContext(
            claims=claims,
            token=x_agent_context or "",
            request_id=x_request_id or "",
        )
    except AgentContextError as exception:
        raise HTTPException(status_code=401, detail=str(exception)) from exception
