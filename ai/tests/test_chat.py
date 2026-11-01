import asyncio
import json

import pytest
from fastapi import HTTPException
from langchain_core.messages import AIMessage

from config.settings import Settings
from integrations.java_api import JavaToolError
from integrations.llm import LlmUnavailableError, create_chat_model
from routers.chat import chat
from schemas.agent import AgentAuthContext, AgentContextClaims
from schemas.chat import ChatHistoryItem, ChatRequest
from services.chat_service import ChatService


class FakeModel:
    def __init__(self, answer: str = "这是模型回答") -> None:
        self.answer = answer
        self.messages = []

    async def ainvoke(self, messages):
        self.messages = messages
        return AIMessage(content=self.answer)


class FakeBusinessTools:
    def __init__(
        self,
        result: dict | None = None,
        error: str | None = None,
    ) -> None:
        self.result = result or {
            "list": [
                {
                    "reservationNo": "R-501",
                    "roomName": "化学实验室 A101",
                }
            ],
            "total": 1,
        }
        self.error = error
        self.auth = None
        self.tool_calls = []
        self.tool_arguments = []

    async def invoke_tool(self, auth, tool_name, arguments=None):
        self.auth = auth
        self.tool_calls.append(tool_name)
        self.tool_arguments.append(arguments)
        if self.error:
            raise JavaToolError(self.error)
        if tool_name == "queryMyCredit":
            return {"score": 100, "minScore": 60, "canBook": True}
        if tool_name == "checkSafetyExamStatus":
            return {"hasValidPass": True}
        if tool_name == "queryAvailableRooms":
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
            return {
                "list": [
                    {
                        "id": 3,
                        "deviceName": "离心机",
                    }
                ],
                "total": 1,
            }
        if tool_name == "createReservationDraft":
            return {
                "id": 70,
                "sessionId": 101,
                "status": "PENDING",
                "request": {
                    "roomId": 12,
                    "deviceIds": [3],
                },
            }
        return self.result


class ReservationFakeModel(FakeModel):
    def __init__(self, extraction: dict, answer: str = "已找到可用实验室") -> None:
        super().__init__(answer)
        self.extraction = extraction
        self.calls = []

    async def ainvoke(self, messages):
        self.calls.append(messages)
        if "预约参数抽取器" in str(messages[0].content):
            return AIMessage(
                content=json.dumps(self.extraction, ensure_ascii=False)
            )
        return AIMessage(content=self.answer)


def settings(api_key: str = "test-key") -> Settings:
    return Settings(
        agent_context_secret="0123456789abcdef0123456789abcdef",
        llm_api_key=api_key,
        llm_base_url="http://127.0.0.1:9999",
        llm_model="test-model",
        java_base_url="http://127.0.0.1:8087",
    )


def context(session_id: int = 101) -> AgentAuthContext:
    return AgentAuthContext(
        claims=AgentContextClaims(
            version=1,
            userId=7,
            role="STUDENT",
            sessionId=session_id,
            traceId="trace-1",
            allowedTools=["queryMyReservations"],
            issuedAt=1,
            expiresAt=2,
        ),
        token="signed-context",
        request_id="trace-1",
    )


def test_chat_service_builds_messages() -> None:
    model = FakeModel()
    business_tools = FakeBusinessTools()
    service = ChatService(settings(), lambda _: model, business_tools)
    request = ChatRequest(
        sessionId=101,
        message="查询我的预约",
        history=[ChatHistoryItem(role="user", content="你好")],
    )

    response = asyncio.run(service.chat(request, context()))

    assert response.answer == "这是模型回答"
    assert response.intent == "BUSINESS_QUERY"
    assert response.session_id == 101
    assert response.trace_id == "trace-1"
    assert response.model == "test-model"
    assert model.messages[0].content.startswith("你是校园实验室")
    assert "R-501" in model.messages[0].content
    assert model.messages[-1].content == "查询我的预约"
    assert business_tools.auth.token == "signed-context"
    assert business_tools.tool_calls == ["queryMyReservations"]
    assert response.tool_calls[0].tool_name == "queryMyReservations"
    assert response.tool_calls[0].status == "succeeded"


def test_chat_route_rejects_session_mismatch() -> None:
    service = ChatService(settings(), lambda _: FakeModel(), FakeBusinessTools())
    request = ChatRequest(sessionId=102, message="你好")

    with pytest.raises(HTTPException) as exception:
        asyncio.run(chat(request, context(101), service))

    assert exception.value.status_code == 403


def test_chat_route_reports_missing_model_configuration() -> None:
    def unavailable(_: Settings):
        raise LlmUnavailableError("模型 API Key 未配置")

    service = ChatService(settings(api_key=""), unavailable, FakeBusinessTools())
    request = ChatRequest(sessionId=101, message="你好")

    with pytest.raises(HTTPException) as exception:
        asyncio.run(chat(request, context(), service))

    assert exception.value.status_code == 503


def test_chat_model_factory_uses_configured_model() -> None:
    model = create_chat_model(settings())

    assert model.model_name == "test-model"


def test_chat_model_factory_requires_api_key() -> None:
    with pytest.raises(LlmUnavailableError):
        create_chat_model(settings(api_key=""))


@pytest.mark.parametrize(
    ("message", "intent"),
    [
        ("实验室着火了怎么办", "SAFETY_KNOWLEDGE"),
        ("我想预约化学实验室", "RESERVATION"),
        ("查询我的预约", "BUSINESS_QUERY"),
        ("你好", "GENERAL"),
    ],
)
def test_chat_service_routes_intent(message: str, intent: str) -> None:
    response = asyncio.run(
        ChatService(settings(), lambda _: FakeModel(), FakeBusinessTools()).chat(
            ChatRequest(sessionId=101, message=message),
            context(),
        )
    )

    assert response.intent == intent


def test_business_tool_failure_returns_controlled_answer() -> None:
    model = FakeModel(answer="不应调用模型")
    service = ChatService(
        settings(),
        lambda _: model,
        FakeBusinessTools(error="Java 服务超时"),
    )

    response = asyncio.run(
        service.chat(
            ChatRequest(sessionId=101, message="查询我的预约"),
            context(),
        )
    )

    assert response.intent == "BUSINESS_QUERY"
    assert "暂时无法查询实时业务数据" in response.answer
    assert response.tool_calls[0].status == "failed"
    assert model.messages == []


@pytest.mark.parametrize(
    ("message", "expected_tools"),
    [
        ("我的信用分是多少", ["queryMyCredit"]),
        ("我的安全考试通过了吗", ["checkSafetyExamStatus"]),
        (
            "我有预约资格吗",
            ["checkSafetyExamStatus", "queryMyCredit"],
        ),
    ],
)
def test_business_query_selects_expected_tools(
    message: str,
    expected_tools: list[str],
) -> None:
    business_tools = FakeBusinessTools()
    service = ChatService(settings(), lambda _: FakeModel(), business_tools)

    asyncio.run(
        service.chat(
            ChatRequest(sessionId=101, message=message),
            context(),
        )
    )

    assert business_tools.tool_calls == expected_tools


def test_complete_reservation_queries_available_rooms() -> None:
    model = ReservationFakeModel(
        {
            "start_time": "2026-11-11 14:00",
            "end_time": "2026-11-11 16:00",
            "participant_count": 2,
            "experiment_name": "萃取实验",
            "room_type": "化学",
            "room_name": None,
            "device_names": [],
        }
    )
    business_tools = FakeBusinessTools()
    service = ChatService(settings(), lambda _: model, business_tools)

    response = asyncio.run(
        service.chat(
            ChatRequest(
                sessionId=101,
                message="下周三下午两点到四点预约化学实验室",
                history=[
                    ChatHistoryItem(
                        role="user",
                        content="这次预约共有两个人",
                    )
                ],
            ),
            context(),
        )
    )

    assert response.intent == "RESERVATION"
    assert response.answer == "已找到可用实验室"
    assert business_tools.tool_calls == ["queryAvailableRooms"]
    assert response.tool_calls[0].tool_name == "queryAvailableRooms"
    assert response.draft is None
    assert business_tools.tool_arguments[0] == {
        "start_time": "2026-11-11 14:00:00",
        "end_time": "2026-11-11 16:00:00",
        "participant_count": 2,
    }
    assert "这次预约共有两个人" in model.calls[0][1].content


def test_incomplete_reservation_asks_without_querying_tools() -> None:
    model = ReservationFakeModel(
        {
            "start_time": None,
            "end_time": None,
            "participant_count": 2,
            "experiment_name": "萃取实验",
            "room_type": "化学",
            "device_names": [],
        }
    )
    business_tools = FakeBusinessTools()
    service = ChatService(settings(), lambda _: model, business_tools)

    response = asyncio.run(
        service.chat(
            ChatRequest(
                sessionId=101,
                message="我想预约化学实验室，两个人",
            ),
            context(),
        )
    )

    assert response.intent == "RESERVATION"
    assert "开始时间" in response.answer
    assert "结束时间" in response.answer
    assert business_tools.tool_calls == []


def test_selected_room_creates_pending_draft() -> None:
    model = ReservationFakeModel(
        {
            "start_time": "2026-11-11 14:00",
            "end_time": "2026-11-11 16:00",
            "participant_count": 2,
            "experiment_name": "萃取实验",
            "room_type": "化学",
            "room_name": "化学实验室 A101",
            "device_names": ["离心机"],
        }
    )
    business_tools = FakeBusinessTools()
    service = ChatService(settings(), lambda _: model, business_tools)

    response = asyncio.run(
        service.chat(
            ChatRequest(
                sessionId=101,
                message="预约化学实验室 A101，使用离心机",
                history=[
                    ChatHistoryItem(
                        role="user",
                        content="预约下周三下午两点到四点，两个人",
                    )
                ],
            ),
            context(),
        )
    )

    assert business_tools.tool_calls == [
        "queryAvailableRooms",
        "queryAvailableDevices",
        "createReservationDraft",
    ]
    assert response.draft["id"] == 70
    assert response.draft["status"] == "PENDING"


def test_stream_chat_emits_tool_and_done_events() -> None:
    service = ChatService(
        settings(),
        lambda _: FakeModel(),
        FakeBusinessTools(),
    )

    async def collect():
        return [
            event
            async for event in service.stream_chat(
                ChatRequest(sessionId=101, message="查询我的预约"),
                context(),
            )
        ]

    events = asyncio.run(collect())

    assert events[0]["type"] == "start"
    assert any(
        event["type"] == "tool"
        and event["toolName"] == "queryMyReservations"
        for event in events
    )
    assert any(event["type"] == "message.delta" for event in events)
    assert events[-1]["type"] == "done"
    assert events[-1]["intent"] == "BUSINESS_QUERY"
    assert events[-1]["toolCalls"][0]["toolName"] == "queryMyReservations"
