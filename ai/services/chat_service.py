from collections.abc import Callable
from functools import lru_cache

from langchain_core.language_models.chat_models import BaseChatModel

from agent.graph import build_agent_graph
from config.settings import Settings, get_settings
from integrations.java_api import JavaApiClient
from integrations.llm import create_chat_model
from schemas.agent import AgentAuthContext
from schemas.chat import ChatRequest, ChatResponse
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
            session_id=request.session_id,
            trace_id=auth.claims.trace_id,
            model=self.settings.llm_model,
        )


@lru_cache
def get_chat_service() -> ChatService:
    return ChatService(get_settings())
