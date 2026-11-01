from typing import TypedDict

from schemas.chat import ChatHistoryItem


class AgentState(TypedDict, total=False):
    message: str
    history: list[ChatHistoryItem]
    intent: str
    answer: str
