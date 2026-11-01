from fastapi import FastAPI

from config.settings import get_settings
from routers.health import router as health_router
from utils.exception_handlers import register_exception_handlers

settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    debug=settings.debug,
)

register_exception_handlers(app)
app.include_router(health_router)
