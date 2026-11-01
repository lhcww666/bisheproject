from collections.abc import Callable
import json

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from agent.state import AgentState
from config.settings import Settings
from integrations.java_api import JavaToolError
from integrations.llm import create_chat_model
from tools.business_tools import BusinessToolService

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
        business_tools: BusinessToolService | None = None,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory
        self.business_tools = business_tools

    def route_intent(self, state: AgentState) -> dict[str, str]:
        message = state["message"].lower()
        if any(
            keyword in message
            for keyword in (
                "我的预约",
                "信用",
                "考试",
                "资格",
                "审核结果",
                "可用实验室",
                "设备状态",
            )
        ):
            intent = "BUSINESS_QUERY"
        elif any(keyword in message for keyword in ("安全", "着火", "泄漏", "防护", "应急")):
            intent = "SAFETY_KNOWLEDGE"
        elif any(keyword in message for keyword in ("预约", "约实验室", "约设备")):
            intent = "RESERVATION"
        else:
            intent = "GENERAL"
        return {"intent": intent}

    async def answer_business_query(self, state: AgentState) -> dict[str, str]:
        if state.get("tool_error"):
            return {
                "answer": (
                    "暂时无法查询实时业务数据。"
                    "请稍后重试，或前往对应业务页面查看。"
                )
            }
        if state.get("tool_result") is not None:
            prompt = (
                PROMPTS["BUSINESS_QUERY"]
                + "以下是 Java 业务工具返回的真实数据，只能据此总结："
                + json.dumps(state["tool_result"], ensure_ascii=False)
            )
            return {"answer": await self._answer(state, prompt)}
        return {"answer": await self._answer(state, PROMPTS["BUSINESS_QUERY"])}

    async def query_business_tool(self, state: AgentState) -> dict:
        if self.business_tools is None:
            return {"tool_error": "业务查询工具未配置"}
        try:
            message = state["message"]
            if "预约资格" in message or "资格" in message:
                tool_calls = [
                    ("checkSafetyExamStatus", {}),
                    ("queryMyCredit", {}),
                ]
            elif "信用" in message:
                tool_calls = [("queryMyCredit", {})]
            elif "考试" in message:
                tool_calls = [("checkSafetyExamStatus", {})]
            else:
                tool_calls = [("queryMyReservations", {})]
            result = {}
            for tool_name, arguments in tool_calls:
                result[tool_name] = await self.business_tools.invoke_tool(
                    state["auth"],
                    tool_name,
                    arguments,
                )
            return {"tool_result": result, "tool_error": None}
        except JavaToolError as exception:
            return {"tool_error": str(exception)}

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
