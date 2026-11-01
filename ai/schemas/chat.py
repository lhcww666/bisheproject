from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class ChatHistoryItem(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=8000)


class ChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    session_id: int = Field(gt=0, alias="sessionId")
    message: str = Field(min_length=1, max_length=8000)
    history: list[ChatHistoryItem] = Field(default_factory=list, max_length=30)


class ChatResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    answer: str
    session_id: int = Field(alias="sessionId")
    trace_id: str = Field(alias="traceId")
    model: str
