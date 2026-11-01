from collections.abc import Callable
from functools import lru_cache

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from config.settings import Settings, get_settings
from integrations.llm import create_chat_model
from schemas.agent import AgentContextClaims
from schemas.chat import ChatRequest, ChatResponse

SYSTEM_PROMPT = (
    "你是校园实验室管理平台的智能助手。"
    "回答应准确、简洁，不编造实验室业务数据。"
    "涉及预约、审核、信用或考试状态时，只有工具返回的数据可以作为事实依据。"
)


class ChatService:
    def __init__(
        self,
        settings: Settings,
        model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory

    async def chat(
        self,
        request: ChatRequest,
        context: AgentContextClaims,
    ) -> ChatResponse:
        model = self.model_factory(self.settings)
        messages = [SystemMessage(content=SYSTEM_PROMPT)]
        messages.extend(
            HumanMessage(content=item.content)
            if item.role == "user"
            else AIMessage(content=item.content)
            for item in request.history
        )
        messages.append(HumanMessage(content=request.message))
        result = await model.ainvoke(messages)
        answer = result.content if isinstance(result.content, str) else str(result.content)
        return ChatResponse(
            answer=answer,
            session_id=request.session_id,
            trace_id=context.trace_id,
            model=self.settings.llm_model,
        )


@lru_cache
def get_chat_service() -> ChatService:
    return ChatService(get_settings())
