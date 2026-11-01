from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException

from knowledge.service import KnowledgeService, get_knowledge_service
from schemas.agent import AgentAuthContext
from schemas.knowledge import KnowledgeSearchRequest, KnowledgeSearchResponse
from utils.auth import require_agent_context

router = APIRouter(prefix="/api/ai", tags=["知识检索"])


@router.post("/knowledge/search", response_model=KnowledgeSearchResponse)
async def search_knowledge(
    request: KnowledgeSearchRequest,
    auth: Annotated[AgentAuthContext, Depends(require_agent_context)],
    service: Annotated[KnowledgeService, Depends(get_knowledge_service)],
) -> KnowledgeSearchResponse:
    if auth.claims.session_id != request.session_id:
        raise HTTPException(status_code=403, detail="会话上下文不匹配")
    return KnowledgeSearchResponse(
        citations=service.search(
            request.query,
            roles={auth.claims.role},
        ),
        session_id=request.session_id,
        trace_id=auth.claims.trace_id,
    )
