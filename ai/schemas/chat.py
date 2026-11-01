import json
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ChatHistoryItem(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=8000)


class ChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    session_id: int = Field(gt=0, alias="sessionId")
    message: str = Field(min_length=1, max_length=8000)
    history: list[ChatHistoryItem] = Field(default_factory=list, max_length=30)

    @field_validator("history", mode="before")
    @classmethod
    def parse_dify_history(cls, value):
        if isinstance(value, str):
            if not value:
                return []
            return json.loads(value)
        return value


class ToolCallSummary(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    tool_name: str = Field(alias="toolName")
    status: Literal["succeeded", "failed"]
    error: str | None = None


class ChatResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    answer: str
    intent: str
    tool_calls: list[ToolCallSummary] = Field(
        default_factory=list,
        alias="toolCalls",
    )
    draft: dict | None = None
    session_id: int = Field(alias="sessionId")
    trace_id: str = Field(alias="traceId")
    model: str
