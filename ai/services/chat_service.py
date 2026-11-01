from collections.abc import Callable
from functools import lru_cache

from langchain_core.language_models.chat_models import BaseChatModel

from agent.graph import build_agent_graph
from config.settings import Settings, get_settings
from integrations.llm import create_chat_model
from schemas.agent import AgentContextClaims
from schemas.chat import ChatRequest, ChatResponse

class ChatService:
    def __init__(
        self,
        settings: Settings,
        model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory
        self.graph = build_agent_graph(settings, model_factory)

    async def chat(
        self,
        request: ChatRequest,
        context: AgentContextClaims,
    ) -> ChatResponse:
        result = await self.graph.ainvoke(
            {
                "message": request.message,
                "history": request.history,
            }
        )
        return ChatResponse(
            answer=result["answer"],
            intent=result["intent"],
            session_id=request.session_id,
            trace_id=context.trace_id,
            model=self.settings.llm_model,
        )


@lru_cache
def get_chat_service() -> ChatService:
    return ChatService(get_settings())
