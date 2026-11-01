from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException

from schemas.agent import AgentAuthContext
from schemas.reservation_analysis import (
    ReservationAnalyzeRequest,
    ReservationAnalyzeResponse,
)
from services.reservation_analysis_service import (
    ReservationAnalysisService,
    get_reservation_analysis_service,
)
from utils.auth import require_agent_context

router = APIRouter(prefix="/api/ai", tags=["预约分析"])


@router.post("/reservation/analyze", response_model=ReservationAnalyzeResponse)
async def analyze_reservation(
    request: ReservationAnalyzeRequest,
    auth: Annotated[AgentAuthContext, Depends(require_agent_context)],
    service: Annotated[
        ReservationAnalysisService,
        Depends(get_reservation_analysis_service),
    ],
) -> ReservationAnalyzeResponse:
    if auth.claims.session_id != request.session_id:
        raise HTTPException(status_code=403, detail="会话上下文不匹配")
    return await service.analyze(request, auth)
