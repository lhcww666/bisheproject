from typing import Literal

from pydantic import BaseModel


class DependencyStatus(BaseModel):
    llm: Literal["ok", "not_configured", "unavailable"] = "not_configured"
    dify_knowledge: Literal["ok", "not_configured", "unavailable"] = (
        "not_configured"
    )
    java_tools: Literal["ok", "not_configured", "unavailable"] = "not_configured"


class HealthResponse(BaseModel):
    status: Literal["ok"] = "ok"
    service: str
    version: str
    dependencies: DependencyStatus
