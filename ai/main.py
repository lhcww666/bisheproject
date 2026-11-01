from fastapi import FastAPI

from config.settings import get_settings
from routers.chat import router as chat_router
from routers.health import router as health_router
from routers.knowledge import router as knowledge_router
from routers.reservation import router as reservation_router
from utils.exception_handlers import register_exception_handlers

settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    debug=settings.debug,
)

register_exception_handlers(app)
app.include_router(health_router)
app.include_router(chat_router)
app.include_router(knowledge_router)
app.include_router(reservation_router)
