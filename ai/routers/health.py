from fastapi import APIRouter

from config.settings import get_settings
from schemas.health import DependencyStatus, HealthResponse

router = APIRouter(prefix="/api/ai", tags=["健康检查"])


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    settings = get_settings()
    return HealthResponse(
        service=settings.app_name,
        version=settings.app_version,
        dependencies=DependencyStatus(),
    )
