from typing import Annotated

from fastapi import Header, HTTPException

from config.settings import get_settings
from schemas.agent import AgentContextClaims
from utils.agent_context import AgentContextError, verify_agent_context


def require_agent_context(
    x_agent_context: Annotated[
        str | None,
        Header(alias="X-Agent-Context"),
    ] = None,
) -> AgentContextClaims:
    try:
        return verify_agent_context(
            x_agent_context or "",
            get_settings().agent_context_secret,
        )
    except AgentContextError as exception:
        raise HTTPException(status_code=401, detail=str(exception)) from exception
