from collections.abc import Callable
import json

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from agent.state import AgentState
from config.settings import Settings
from integrations.java_api import JavaToolError
from integrations.llm import create_chat_model
from knowledge.service import KnowledgeService
from services.reservation_service import (
    ReservationExtractionError,
    ReservationExtractionService,
    missing_field_message,
)
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
        + "只能根据可约资源工具返回的数据介绍实验室。"
        "工具没有返回的实验室、设备或时段不能声称可预约。"
    ),
    "SAFETY_KNOWLEDGE": (
        BASE_PROMPT
        + "只能依据下方知识片段回答，不得补充片段中没有的安全步骤、设备参数或医疗建议。"
        "引用知识片段时标注对应编号，例如[1]。"
        "如果片段不足以回答，应明确说明，并建议用户查阅正式安全规程或联系实验室管理员。"
    ),
    "GENERAL": BASE_PROMPT,
}


class AgentNodes:
    def __init__(
        self,
        settings: Settings,
        model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
        business_tools: BusinessToolService | None = None,
        knowledge_service: KnowledgeService | None = None,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory
        self.business_tools = business_tools
        self.knowledge_service = knowledge_service
        self.reservation_extractor = ReservationExtractionService(
            settings,
            model_factory,
        )

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
        elif any(
            keyword in message
            for keyword in (
                "安全",
                "着火",
                "泄漏",
                "防护",
                "应急",
                "气体",
                "化学品",
                "灭火",
                "洗眼器",
                "危险废物",
            )
        ):
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
            return {"tool_error": "业务查询工具未配置", "tool_calls": []}
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
            events = []
            for tool_name, arguments in tool_calls:
                result[tool_name] = await self.business_tools.invoke_tool(
                    state["auth"],
                    tool_name,
                    arguments,
                )
                events.append(
                    {
                        "toolName": tool_name,
                        "status": "succeeded",
                        "error": None,
                    }
                )
            return {
                "tool_result": result,
                "tool_error": None,
                "tool_calls": events,
            }
        except JavaToolError as exception:
            return {
                "tool_error": str(exception),
                "tool_calls": [
                    {
                        "toolName": tool_calls[-1][0],
                        "status": "failed",
                        "error": str(exception),
                    }
                ],
            }

    async def answer_reservation(self, state: AgentState) -> dict[str, str]:
        if state.get("tool_error"):
            return {
                "answer": (
                    "暂时无法查询可用实验室。"
                    "请稍后重试，或前往预约页面查询。"
                )
            }
        rooms = state.get("available_rooms")
        if rooms is not None:
            prompt = (
                PROMPTS["RESERVATION"]
                + "以下是可用实验室工具返回的结果："
                + json.dumps(rooms, ensure_ascii=False)
            )
            if state.get("draft") is not None:
                prompt += (
                    "预约草稿已经创建，但尚未提交。"
                    "请要求用户在前端确认草稿后再创建正式预约。"
                    + json.dumps(state["draft"], ensure_ascii=False)
                )
            elif not state.get("reservation").room_name:
                prompt += "请列出候选实验室，并要求用户明确选择其中一个。"
            return {"answer": await self._answer(state, prompt)}
        return {"answer": await self._answer(state, PROMPTS["RESERVATION"])}

    async def extract_reservation(self, state: AgentState) -> dict:
        try:
            reservation, missing_fields = await self.reservation_extractor.extract(
                state["message"],
                state.get("history", []),
            )
            return {
                "reservation": reservation,
                "missing_fields": missing_fields,
            }
        except ReservationExtractionError:
            return {
                "missing_fields": ["start_time", "end_time", "participant_count"],
            }

    def next_reservation_step(self, state: AgentState) -> str:
        return "ask" if state.get("missing_fields") else "query"

    async def ask_reservation_fields(self, state: AgentState) -> dict[str, str]:
        return {"answer": missing_field_message(state.get("missing_fields", []))}

    async def query_reservation_rooms(self, state: AgentState) -> dict:
        if self.business_tools is None:
            return {
                "tool_error": "可用实验室工具未配置",
                "tool_calls": [],
            }
        reservation = state["reservation"]
        try:
            rooms = await self.business_tools.invoke_tool(
                state["auth"],
                "queryAvailableRooms",
                {
                    "start_time": reservation.start_time,
                    "end_time": reservation.end_time,
                    "participant_count": reservation.participant_count,
                },
            )
            return {
                "available_rooms": rooms,
                "tool_error": None,
                "tool_calls": [
                    {
                        "toolName": "queryAvailableRooms",
                        "status": "succeeded",
                        "error": None,
                    }
                ],
            }
        except Exception as exception:
            return {
                "tool_error": str(exception),
                "tool_calls": [
                    {
                        "toolName": "queryAvailableRooms",
                        "status": "failed",
                        "error": str(exception),
                    }
                ],
            }

    def next_after_room_query(self, state: AgentState) -> str:
        if state.get("tool_error") or not state.get("reservation").room_name:
            return "answer"
        return "draft"

    async def create_reservation_draft(self, state: AgentState) -> dict:
        if self.business_tools is None:
            return {
                "tool_error": "预约草稿工具未配置",
                "tool_calls": [],
            }
        reservation = state["reservation"]
        room = _match_room(state.get("available_rooms") or {}, reservation.room_name)
        if room is None:
            return {
                "tool_error": "没有找到用户明确选择的可用实验室",
                "tool_calls": [
                    {
                        "toolName": "createReservationDraft",
                        "status": "failed",
                        "error": "所选实验室不在可用列表中",
                    }
                ],
            }

        events = []
        device_ids = []
        if reservation.device_names:
            try:
                devices = await self.business_tools.invoke_tool(
                    state["auth"],
                    "queryAvailableDevices",
                    {"room_id": room["id"]},
                )
                events.append(
                    {
                        "toolName": "queryAvailableDevices",
                        "status": "succeeded",
                        "error": None,
                    }
                )
                device_ids = _match_devices(devices, reservation.device_names)
                if device_ids is None:
                    return {
                        "tool_error": "所需设备在该实验室不可用",
                        "tool_calls": events,
                    }
            except Exception as exception:
                events.append(
                    {
                        "toolName": "queryAvailableDevices",
                        "status": "failed",
                        "error": str(exception),
                    }
                )
                return {
                    "tool_error": str(exception),
                    "tool_calls": events,
                }

        try:
            draft = await self.business_tools.invoke_tool(
                state["auth"],
                "createReservationDraft",
                {
                    "session_id": state["auth"].claims.session_id,
                    "room_id": room["id"],
                    "experiment_name": reservation.experiment_name
                    or "实验室预约",
                    "start_time": reservation.start_time,
                    "end_time": reservation.end_time,
                    "participant_count": reservation.participant_count,
                    "device_ids": device_ids,
                },
            )
            events.append(
                {
                    "toolName": "createReservationDraft",
                    "status": "succeeded",
                    "error": None,
                }
            )
            return {
                "draft": draft,
                "tool_error": None,
                "tool_calls": events,
            }
        except Exception as exception:
            events.append(
                {
                    "toolName": "createReservationDraft",
                    "status": "failed",
                    "error": str(exception),
                }
            )
            return {
                "tool_error": str(exception),
                "tool_calls": events,
            }
        reservation = state["reservation"]
        try:
            rooms = await self.business_tools.invoke_tool(
                state["auth"],
                "queryAvailableRooms",
                {
                    "start_time": reservation.start_time,
                    "end_time": reservation.end_time,
                    "participant_count": reservation.participant_count,
                },
            )
            return {
                "available_rooms": rooms,
                "tool_error": None,
                "tool_calls": [
                    {
                        "toolName": "queryAvailableRooms",
                        "status": "succeeded",
                        "error": None,
                    }
                ],
            }
        except Exception as exception:
            return {
                "tool_error": str(exception),
                "tool_calls": [
                    {
                        "toolName": "queryAvailableRooms",
                        "status": "failed",
                        "error": str(exception),
                    }
                ],
            }

    def retrieve_safety_knowledge(self, state: AgentState) -> dict:
        if self.knowledge_service is None:
            return {"citations": []}
        return {"citations": self.knowledge_service.search(state["message"])}

    async def answer_safety(self, state: AgentState) -> dict[str, str]:
        citations = state.get("citations", [])
        if not citations:
            return {
                "answer": (
                    "当前知识库中没有找到足以支持该问题的可靠资料。"
                    "请查阅实验室正式安全规程，或联系实验室管理员确认。"
                    "在核实前不要尝试未经说明的危险操作。"
                )
            }
        context = "\n\n".join(
            (
                f"[{index}] {citation.title}（{citation.source}）\n"
                f"{citation.snippet}"
            )
            for index, citation in enumerate(citations, start=1)
        )
        prompt = PROMPTS["SAFETY_KNOWLEDGE"] + "\n\n知识片段：\n" + context
        return {"answer": await self._answer(state, prompt)}

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


def _match_room(rooms: dict, requested: str) -> dict | None:
    normalized = requested.strip().casefold()
    for room in rooms.get("list", []):
        name = str(room.get("roomName", "")).strip().casefold()
        code = str(room.get("roomCode", "")).strip().casefold()
        if normalized in {name, code} or normalized in name or normalized in code:
            return room
    return None


def _match_devices(devices: dict, requested_names: list[str]) -> list[int] | None:
    available = devices.get("list", [])
    resolved = []
    for requested in requested_names:
        normalized = requested.strip().casefold()
        match = next(
            (
                device
                for device in available
                if normalized
                in str(device.get("deviceName", "")).strip().casefold()
            ),
            None,
        )
        if match is None or not match.get("id"):
            return None
        resolved.append(match["id"])
    return list(dict.fromkeys(resolved))
