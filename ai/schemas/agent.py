from pydantic import BaseModel, ConfigDict, Field


class AgentContextClaims(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    version: int
    user_id: int = Field(alias="userId")
    role: str
    session_id: int = Field(alias="sessionId")
    trace_id: str = Field(alias="traceId")
    allowed_tools: list[str] = Field(default_factory=list, alias="allowedTools")
    issued_at: int = Field(alias="issuedAt")
    expires_at: int = Field(alias="expiresAt")
