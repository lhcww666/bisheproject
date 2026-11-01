import asyncio

from routers.health import health


def test_health_returns_service_status() -> None:
    body = asyncio.run(health())

    assert body.status == "ok"
    assert body.service == "校园实验室 Python Agent 服务"
    assert body.version == "0.1.0"
    assert body.dependencies.llm == "not_configured"
