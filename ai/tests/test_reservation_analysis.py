import asyncio
import json

from langchain_core.messages import AIMessage

from config.settings import Settings
from integrations.java_api import JavaToolError
from schemas.agent import AgentAuthContext, AgentContextClaims
from schemas.reservation_analysis import ReservationAnalyzeRequest
from services.reservation_analysis_service import ReservationAnalysisService


class ExtractionModel:
    def __init__(self, payload: dict) -> None:
        self.payload = payload

    async def ainvoke(self, messages):
        return AIMessage(content=json.dumps(self.payload, ensure_ascii=False))


class FakeBusinessTools:
    def __init__(self, room_error: str | None = None) -> None:
        self.room_error = room_error

    async def invoke_tool(self, auth, tool_name, arguments=None):
        if tool_name == "queryAvailableRooms":
            if self.room_error:
                raise JavaToolError(self.room_error)
            return {
                "list": [
                    {
                        "id": 12,
                        "roomCode": "CHEM-A101",
                        "roomName": "化学实验室 A101",
                    }
                ],
                "total": 1,
            }
        if tool_name == "queryAvailableDevices":
            return {"list": [{"id": 3, "deviceName": "离心机"}], "total": 1}
        if tool_name == "createReservationDraft":
            return {"id": 70, "status": "PENDING"}
        return {}


def settings() -> Settings:
    return Settings(
        agent_context_secret="0123456789abcdef0123456789abcdef",
        llm_api_key="test-key",
        llm_model="test-model",
        java_base_url="http://127.0.0.1:8087",
    )


def auth() -> AgentAuthContext:
    return AgentAuthContext(
        claims=AgentContextClaims(
            version=1,
            userId=7,
            role="STUDENT",
            sessionId=101,
            traceId="trace-1",
            allowedTools=["queryAvailableRooms", "createReservationDraft"],
            issuedAt=1,
            expiresAt=2,
        ),
        token="signed-context",
        request_id="trace-1",
    )


def extraction(**overrides) -> dict:
    payload = {
        "start_time": "2026-11-11 14:00",
        "end_time": "2026-11-11 16:00",
        "participant_count": 2,
        "experiment_name": "萃取实验",
        "room_type": "化学",
        "room_name": None,
        "device_names": [],
    }
    payload.update(overrides)
    return payload


def test_analysis_returns_missing_fields() -> None:
    service = ReservationAnalysisService(
        settings(),
        FakeBusinessTools(),
    )
    service.nodes.reservation_extractor.model_factory = lambda _: ExtractionModel(
        extraction(start_time=None, end_time=None)
    )

    response = asyncio.run(
        service.analyze(
            ReservationAnalyzeRequest(sessionId=101, message="我想预约实验室"),
            auth(),
        )
    )

    assert response.status == "need_more_info"
    assert response.missing_fields == ["start_time", "end_time"]


def test_analysis_returns_room_choice() -> None:
    service = ReservationAnalysisService(settings(), FakeBusinessTools())
    service.nodes.reservation_extractor.model_factory = lambda _: ExtractionModel(extraction())

    response = asyncio.run(
        service.analyze(
            ReservationAnalyzeRequest(sessionId=101, message="预约化学实验室"),
            auth(),
        )
    )

    assert response.status == "room_choice"
    assert response.available_rooms[0]["id"] == 12


def test_analysis_creates_draft_after_room_selection() -> None:
    service = ReservationAnalysisService(settings(), FakeBusinessTools())
    service.nodes.reservation_extractor.model_factory = lambda _: ExtractionModel(
        extraction(room_name="化学实验室 A101", device_names=["离心机"])
    )

    response = asyncio.run(
        service.analyze(
            ReservationAnalyzeRequest(sessionId=101, message="选择 A101"),
            auth(),
        )
    )

    assert response.status == "draft_ready"
    assert response.draft["id"] == 70
    assert [call.tool_name for call in response.tool_calls] == [
        "queryAvailableRooms",
        "queryAvailableDevices",
        "createReservationDraft",
    ]


def test_analysis_distinguishes_ineligible_result() -> None:
    service = ReservationAnalysisService(
        settings(),
        FakeBusinessTools(room_error="未通过安全考试"),
    )
    service.nodes.reservation_extractor.model_factory = lambda _: ExtractionModel(extraction())

    response = asyncio.run(
        service.analyze(
            ReservationAnalyzeRequest(sessionId=101, message="预约化学实验室"),
            auth(),
        )
    )

    assert response.status == "ineligible"
    assert response.reply == "未通过安全考试"
