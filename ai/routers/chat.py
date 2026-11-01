from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException

from integrations.llm import LlmUnavailableError
from schemas.agent import AgentContextClaims
from schemas.chat import ChatRequest, ChatResponse
from services.chat_service import ChatService, get_chat_service
from utils.auth import require_agent_context

router = APIRouter(prefix="/api/ai", tags=["智能对话"])


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    context: Annotated[AgentContextClaims, Depends(require_agent_context)],
    service: Annotated[ChatService, Depends(get_chat_service)],
) -> ChatResponse:
    if context.session_id != request.session_id:
        raise HTTPException(status_code=403, detail="会话上下文不匹配")
    try:
        return await service.chat(request, context)
    except LlmUnavailableError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=502, detail="模型服务调用失败") from exception
