import asyncio

from schemas.agent import AgentAuthContext, AgentContextClaims
from tools.business_tools import BusinessToolService


class FakeJavaApiClient:
    def __init__(self) -> None:
        self.calls = []

    async def get_my_reservations(
        self,
        context_token: str,
        request_id: str,
        status: str | None = None,
    ) -> dict:
        self.calls.append((context_token, request_id, status))
        return {"list": [], "total": 0}


def test_langchain_tool_uses_signed_context() -> None:
    java_api = FakeJavaApiClient()
    service = BusinessToolService(java_api)
    auth = AgentAuthContext(
        claims=AgentContextClaims(
            version=1,
            userId=7,
            role="STUDENT",
            sessionId=101,
            traceId="trace-1",
            allowedTools=["queryMyReservations"],
            issuedAt=1,
            expiresAt=2,
        ),
        token="signed-context",
        request_id="trace-1",
    )

    result = asyncio.run(service.invoke_query_my_reservations(auth))

    assert result == {"list": [], "total": 0}
    assert java_api.calls == [("signed-context", "trace-1", None)]
