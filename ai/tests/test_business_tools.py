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

    async def check_safety_exam_status(
        self,
        context_token: str,
        request_id: str,
    ) -> dict:
        self.calls.append((context_token, request_id, "safety"))
        return {"hasValidPass": True}

    async def query_my_credit(
        self,
        context_token: str,
        request_id: str,
    ) -> dict:
        self.calls.append((context_token, request_id, "credit"))
        return {"score": 100, "minScore": 60, "canBook": True}


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

    result = asyncio.run(
        service.invoke_tool(auth, "queryMyReservations")
    )

    assert result == {"list": [], "total": 0}
    assert java_api.calls == [("signed-context", "trace-1", None)]


def test_langchain_toolbox_exposes_credit_and_safety_tools() -> None:
    java_api = FakeJavaApiClient()
    service = BusinessToolService(java_api)
    auth = _auth()

    names = {tool.name for tool in service.langchain_tools(auth)}
    credit = asyncio.run(service.invoke_tool(auth, "queryMyCredit"))
    safety = asyncio.run(service.invoke_tool(auth, "checkSafetyExamStatus"))

    assert names == {
        "queryMyReservations",
        "queryMyCredit",
        "checkSafetyExamStatus",
    }
    assert credit["score"] == 100
    assert safety["hasValidPass"] is True


def _auth() -> AgentAuthContext:
    return AgentAuthContext(
        claims=AgentContextClaims(
            version=1,
            userId=7,
            role="STUDENT",
            sessionId=101,
            traceId="trace-1",
            allowedTools=[
                "queryMyReservations",
                "queryMyCredit",
                "checkSafetyExamStatus",
            ],
            issuedAt=1,
            expiresAt=2,
        ),
        token="signed-context",
        request_id="trace-1",
    )
