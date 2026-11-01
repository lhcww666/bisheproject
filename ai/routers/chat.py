import json
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from integrations.llm import LlmUnavailableError
from schemas.agent import AgentAuthContext
from schemas.chat import ChatRequest, ChatResponse
from services.chat_service import ChatService, get_chat_service
from utils.auth import require_agent_context

router = APIRouter(prefix="/api/ai", tags=["智能对话"])


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    auth: Annotated[AgentAuthContext, Depends(require_agent_context)],
    service: Annotated[ChatService, Depends(get_chat_service)],
) -> ChatResponse:
    if auth.claims.session_id != request.session_id:
        raise HTTPException(status_code=403, detail="会话上下文不匹配")
    try:
        return await service.chat(request, auth)
    except LlmUnavailableError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="模型服务调用失败") from exception


@router.post("/chat/stream")
async def chat_stream(
    request: ChatRequest,
    auth: Annotated[AgentAuthContext, Depends(require_agent_context)],
    service: Annotated[ChatService, Depends(get_chat_service)],
) -> StreamingResponse:
    if auth.claims.session_id != request.session_id:
        raise HTTPException(status_code=403, detail="会话上下文不匹配")

    async def events():
        async for event in service.stream_chat(request, auth):
            event_name = str(event.get("type", "message")).split(".", 1)[0]
            yield (
                f"event: {event_name}\n"
                f"data: {json.dumps(event, ensure_ascii=False)}\n\n"
            )

    return StreamingResponse(
        events(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )
