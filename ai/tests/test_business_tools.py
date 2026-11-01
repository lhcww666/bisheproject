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

    async def query_available_rooms(
        self,
        context_token: str,
        request_id: str,
        start_time: str,
        end_time: str,
        participant_count: int | None = None,
    ) -> dict:
        self.calls.append(
            (
                context_token,
                request_id,
                "rooms",
                start_time,
                end_time,
                participant_count,
            )
        )
        return {"list": [{"roomName": "化学实验室 A101"}], "total": 1}

    async def query_available_devices(
        self,
        context_token: str,
        request_id: str,
        room_id: int,
    ) -> dict:
        self.calls.append((context_token, request_id, "devices", room_id))
        return {"list": [{"deviceName": "离心机"}], "total": 1}

    async def create_reservation_draft(
        self,
        context_token: str,
        request_id: str,
        payload: dict,
    ) -> dict:
        self.calls.append((context_token, request_id, "draft", payload))
        return {
            "id": 70,
            "sessionId": payload["sessionId"],
            "status": "PENDING",
            "request": payload,
        }


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
        "queryAvailableRooms",
        "queryAvailableDevices",
        "createReservationDraft",
    }
    assert credit["score"] == 100
    assert safety["hasValidPass"] is True


def test_langchain_toolbox_queries_rooms_and_devices() -> None:
    java_api = FakeJavaApiClient()
    service = BusinessToolService(java_api)
    auth = _auth()

    rooms = asyncio.run(
        service.invoke_tool(
            auth,
            "queryAvailableRooms",
            {
                "start_time": "2026-11-08 14:00:00",
                "end_time": "2026-11-08 16:00:00",
                "participant_count": 2,
            },
        )
    )
    devices = asyncio.run(
        service.invoke_tool(
            auth,
            "queryAvailableDevices",
            {"room_id": 12},
        )
    )

    assert rooms["total"] == 1
    assert devices["total"] == 1


def test_langchain_toolbox_creates_pending_draft() -> None:
    java_api = FakeJavaApiClient()
    service = BusinessToolService(java_api)
    auth = _auth()

    draft = asyncio.run(
        service.invoke_tool(
            auth,
            "createReservationDraft",
            {
                "session_id": 101,
                "room_id": 12,
                "experiment_name": "有机溶剂萃取",
                "start_time": "2026-11-08 14:00:00",
                "end_time": "2026-11-08 16:00:00",
                "participant_count": 2,
                "device_ids": [3],
            },
        )
    )

    assert draft["status"] == "PENDING"
    assert draft["request"]["roomId"] == 12


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
