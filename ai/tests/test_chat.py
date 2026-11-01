import asyncio

import pytest
from fastapi import HTTPException
from langchain_core.messages import AIMessage

from config.settings import Settings
from integrations.llm import LlmUnavailableError, create_chat_model
from routers.chat import chat
from schemas.agent import AgentContextClaims
from schemas.chat import ChatHistoryItem, ChatRequest
from services.chat_service import ChatService


class FakeModel:
    def __init__(self, answer: str = "这是模型回答") -> None:
        self.answer = answer
        self.messages = []

    async def ainvoke(self, messages):
        self.messages = messages
        return AIMessage(content=self.answer)


def settings(api_key: str = "test-key") -> Settings:
    return Settings(
        agent_context_secret="0123456789abcdef0123456789abcdef",
        llm_api_key=api_key,
        llm_base_url="http://127.0.0.1:9999",
        llm_model="test-model",
    )


def context(session_id: int = 101) -> AgentContextClaims:
    return AgentContextClaims(
        version=1,
        userId=7,
        role="STUDENT",
        sessionId=session_id,
        traceId="trace-1",
        allowedTools=[],
        issuedAt=1,
        expiresAt=2,
    )


def test_chat_service_builds_messages() -> None:
    model = FakeModel()
    service = ChatService(settings(), lambda _: model)
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
    assert model.messages[-1].content == "查询我的预约"


def test_chat_route_rejects_session_mismatch() -> None:
    service = ChatService(settings(), lambda _: FakeModel())
    request = ChatRequest(sessionId=102, message="你好")

    with pytest.raises(HTTPException) as exception:
        asyncio.run(chat(request, context(101), service))

    assert exception.value.status_code == 403


def test_chat_route_reports_missing_model_configuration() -> None:
    def unavailable(_: Settings):
        raise LlmUnavailableError("模型 API Key 未配置")

    service = ChatService(settings(api_key=""), unavailable)
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
        ChatService(settings(), lambda _: FakeModel()).chat(
            ChatRequest(sessionId=101, message=message),
            context(),
        )
    )

    assert response.intent == intent
