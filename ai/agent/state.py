from typing import TypedDict

from schemas.agent import AgentAuthContext
from schemas.chat import ChatHistoryItem


class AgentState(TypedDict, total=False):
    message: str
    history: list[ChatHistoryItem]
    auth: AgentAuthContext
    intent: str
    tool_result: dict | None
    tool_error: str | None
    answer: str
