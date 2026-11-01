import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(HTTPException)
    async def handle_http_exception(
        request: Request, exception: HTTPException
    ) -> JSONResponse:
        return JSONResponse(
            status_code=exception.status_code,
            content={
                "code": exception.status_code,
                "msg": str(exception.detail),
                "data": {"path": request.url.path},
            },
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        request: Request, exception: RequestValidationError
    ) -> JSONResponse:
        return JSONResponse(
            status_code=422,
            content={
                "code": 422,
                "msg": "请求参数校验失败",
                "data": {
                    "path": request.url.path,
                    "errors": exception.errors(),
                },
            },
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_exception(
        request: Request, exception: Exception
    ) -> JSONResponse:
        logger.exception("未处理异常: %s", request.url.path, exc_info=exception)
        return JSONResponse(
            status_code=500,
            content={
                "code": 500,
                "msg": "Agent 服务内部错误",
                "data": {"path": request.url.path},
            },
        )
