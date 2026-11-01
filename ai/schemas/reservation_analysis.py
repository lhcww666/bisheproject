from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

from schemas.chat import ChatHistoryItem, ToolCallSummary


class ReservationAnalyzeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    session_id: int = Field(gt=0, alias="sessionId")
    message: str = Field(min_length=1, max_length=8000)
    history: list[ChatHistoryItem] = Field(default_factory=list, max_length=30)


class ReservationAnalyzeResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    status: Literal[
        "need_more_info",
        "room_choice",
        "draft_ready",
        "ineligible",
        "no_resource",
        "tool_error",
    ]
    reply: str
    missing_fields: list[str] = Field(default_factory=list, alias="missingFields")
    available_rooms: list[dict] = Field(default_factory=list, alias="availableRooms")
    draft: dict | None = None
    tool_calls: list[ToolCallSummary] = Field(
        default_factory=list,
        alias="toolCalls",
    )
    trace_id: str = Field(alias="traceId")
