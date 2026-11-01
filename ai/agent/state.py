import operator
from typing import Annotated, TypedDict

from schemas.agent import AgentAuthContext
from schemas.chat import ChatHistoryItem
from schemas.reservation import ReservationExtraction


class AgentState(TypedDict, total=False):
    message: str
    history: list[ChatHistoryItem]
    auth: AgentAuthContext
    intent: str
    tool_result: dict | None
    tool_error: str | None
    tool_calls: Annotated[list[dict], operator.add]
    reservation: ReservationExtraction
    missing_fields: list[str]
    available_rooms: dict | None
    answer: str
