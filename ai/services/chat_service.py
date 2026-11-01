from collections.abc import Callable
from collections.abc import AsyncIterator
from functools import lru_cache

from langchain_core.language_models.chat_models import BaseChatModel

from agent.graph import build_agent_graph
from config.settings import Settings, get_settings
from integrations.java_api import JavaApiClient
from integrations.llm import create_chat_model
from schemas.agent import AgentAuthContext
from schemas.chat import ChatRequest, ChatResponse, ToolCallSummary
from tools.business_tools import BusinessToolService

class ChatService:
    def __init__(
        self,
        settings: Settings,
        model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
        business_tools: BusinessToolService | None = None,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory
        self.business_tools = business_tools or BusinessToolService(
            JavaApiClient(settings.java_base_url)
        )
        self.graph = build_agent_graph(
            settings,
            model_factory,
            self.business_tools,
        )

    async def chat(
        self,
        request: ChatRequest,
        auth: AgentAuthContext,
    ) -> ChatResponse:
        result = await self.graph.ainvoke(
            {
                "message": request.message,
                "history": request.history,
                "auth": auth,
            }
        )
        return ChatResponse(
            answer=result["answer"],
            intent=result["intent"],
            tool_calls=[
                ToolCallSummary.model_validate(tool_call)
                for tool_call in result.get("tool_calls", [])
            ],
            draft=result.get("draft"),
            session_id=request.session_id,
            trace_id=auth.claims.trace_id,
            model=self.settings.llm_model,
        )

    async def stream_chat(
        self,
        request: ChatRequest,
        auth: AgentAuthContext,
    ) -> AsyncIterator[dict]:
        yield {
            "type": "start",
            "sessionId": request.session_id,
            "traceId": auth.claims.trace_id,
        }
        answer = ""
        intent = ""
        tool_calls = []
        draft = None
        try:
            async for update in self.graph.astream(
                {
                    "message": request.message,
                    "history": request.history,
                    "auth": auth,
                },
                stream_mode="updates",
            ):
                for node_update in update.values():
                    if not isinstance(node_update, dict):
                        continue
                    if node_update.get("intent"):
                        intent = node_update["intent"]
                    if node_update.get("draft") is not None:
                        draft = node_update["draft"]
                    for tool_call in node_update.get("tool_calls", []):
                        tool_calls.append(tool_call)
                        yield {
                            "type": "tool",
                            "sessionId": request.session_id,
                            "traceId": auth.claims.trace_id,
                            **tool_call,
                        }
                    if node_update.get("answer"):
                        answer = node_update["answer"]
                        yield {
                            "type": "message.delta",
                            "content": answer,
                            "intent": intent,
                        }
        except Exception:
            yield {
                "type": "error",
                "errorCode": "AGENT_STREAM_FAILED",
                "message": "助手响应失败，请稍后重试",
                "retryable": True,
            }
            return

        if not answer:
            yield {
                "type": "error",
                "errorCode": "AGENT_EMPTY_RESPONSE",
                "message": "助手没有生成有效回答",
                "retryable": True,
            }
            return
        yield {
            "type": "done",
            "sessionId": request.session_id,
            "traceId": auth.claims.trace_id,
            "answer": answer,
            "intent": intent,
            "toolCalls": tool_calls,
            "draft": draft,
            "model": self.settings.llm_model,
        }


@lru_cache
def get_chat_service() -> ChatService:
    return ChatService(get_settings())
