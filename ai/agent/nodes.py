from collections.abc import Callable

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from agent.state import AgentState
from config.settings import Settings
from integrations.llm import create_chat_model

BASE_PROMPT = (
    "你是校园实验室管理平台的智能助手。"
    "回答应准确、简洁，不编造实验室业务数据。"
    "涉及预约、审核、信用和考试状态时，只有工具返回的数据可以作为事实依据。"
)

PROMPTS = {
    "BUSINESS_QUERY": (
        BASE_PROMPT
        + "当前业务查询工具尚未接入。不得猜测任何预约、考试、信用或审核数据，"
        "请明确说明暂时无法查询实时业务数据。"
    ),
    "RESERVATION": (
        BASE_PROMPT
        + "当前可约资源工具尚未接入。不得声称实验室或设备可预约，"
        "只能帮助用户整理预约需求，并说明提交前必须由系统重新校验。"
    ),
    "SAFETY_KNOWLEDGE": (
        BASE_PROMPT
        + "当前安全知识库检索尚未接入。不得编造具体安全操作步骤，"
        "应建议用户查阅正式安全规程或联系实验室管理员。"
    ),
    "GENERAL": BASE_PROMPT,
}


class AgentNodes:
    def __init__(
        self,
        settings: Settings,
        model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory

    def route_intent(self, state: AgentState) -> dict[str, str]:
        message = state["message"].lower()
        if any(keyword in message for keyword in ("安全", "着火", "泄漏", "防护", "应急")):
            intent = "SAFETY_KNOWLEDGE"
        elif any(
            keyword in message
            for keyword in ("我的预约", "信用", "考试状态", "审核结果", "可用实验室", "设备状态")
        ):
            intent = "BUSINESS_QUERY"
        elif any(keyword in message for keyword in ("预约", "约实验室", "约设备")):
            intent = "RESERVATION"
        else:
            intent = "GENERAL"
        return {"intent": intent}

    async def answer_business_query(self, state: AgentState) -> dict[str, str]:
        return {"answer": await self._answer(state, PROMPTS["BUSINESS_QUERY"])}

    async def answer_reservation(self, state: AgentState) -> dict[str, str]:
        return {"answer": await self._answer(state, PROMPTS["RESERVATION"])}

    async def answer_safety(self, state: AgentState) -> dict[str, str]:
        return {"answer": await self._answer(state, PROMPTS["SAFETY_KNOWLEDGE"])}

    async def answer_general(self, state: AgentState) -> dict[str, str]:
        return {"answer": await self._answer(state, PROMPTS["GENERAL"])}

    async def _answer(self, state: AgentState, prompt: str) -> str:
        messages = [SystemMessage(content=prompt)]
        messages.extend(
            HumanMessage(content=item.content)
            if item.role == "user"
            else AIMessage(content=item.content)
            for item in state.get("history", [])
        )
        messages.append(HumanMessage(content=state["message"]))
        result = await self.model_factory(self.settings).ainvoke(messages)
        return result.content if isinstance(result.content, str) else str(result.content)
