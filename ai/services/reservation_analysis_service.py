from functools import lru_cache

from agent.nodes import AgentNodes
from config.settings import Settings, get_settings
from integrations.java_api import JavaApiClient
from schemas.agent import AgentAuthContext
from schemas.chat import ToolCallSummary
from schemas.reservation_analysis import (
    ReservationAnalyzeRequest,
    ReservationAnalyzeResponse,
)
from services.reservation_service import missing_field_message
from tools.business_tools import BusinessToolService


class ReservationAnalysisService:
    def __init__(
        self,
        settings: Settings,
        business_tools: BusinessToolService | None = None,
    ) -> None:
        self.settings = settings
        self.business_tools = business_tools or BusinessToolService(
            JavaApiClient(settings.java_base_url)
        )
        self.nodes = AgentNodes(
            settings=settings,
            business_tools=self.business_tools,
        )

    async def analyze(
        self,
        request: ReservationAnalyzeRequest,
        auth: AgentAuthContext,
    ) -> ReservationAnalyzeResponse:
        state = {
            "message": request.message,
            "history": request.history,
            "auth": auth,
        }
        state.update(await self.nodes.extract_reservation(state))
        missing_fields = state.get("missing_fields", [])
        if missing_fields:
            return self._response(
                auth,
                "need_more_info",
                missing_field_message(missing_fields),
                missing_fields=missing_fields,
                tool_calls=[],
            )

        state.update(await self.nodes.query_reservation_rooms(state))
        tool_calls = list(state.get("tool_calls", []))
        if state.get("tool_error"):
            return self._failure_response(auth, state, tool_calls)

        rooms = state.get("available_rooms", {}).get("list", [])
        if not rooms:
            return self._response(
                auth,
                "no_resource",
                "该时段没有满足条件的可用实验室。",
                tool_calls=tool_calls,
            )
        if not state["reservation"].room_name:
            names = "、".join(
                room.get("roomName") or room.get("roomCode") or "未命名实验室"
                for room in rooms
            )
            return self._response(
                auth,
                "room_choice",
                f"找到以下可用实验室：{names}。请明确选择其中一个。",
                available_rooms=rooms,
                tool_calls=tool_calls,
            )

        existing_tool_calls = list(state.get("tool_calls", []))
        draft_update = await self.nodes.create_reservation_draft(state)
        state.update(draft_update)
        tool_calls = existing_tool_calls + list(draft_update.get("tool_calls", []))
        if state.get("tool_error"):
            return self._failure_response(auth, state, tool_calls)
        return self._response(
            auth,
            "draft_ready",
            "预约草稿已生成，请在页面确认后提交正式预约。",
            available_rooms=rooms,
            draft=state.get("draft"),
            tool_calls=tool_calls,
        )

    def _failure_response(
        self,
        auth: AgentAuthContext,
        state: dict,
        tool_calls: list[dict],
    ) -> ReservationAnalyzeResponse:
        error = str(state.get("tool_error", "预约分析失败"))
        status = (
            "ineligible"
            if any(keyword in error for keyword in ("考试", "信用", "准入", "资格"))
            else "tool_error"
        )
        return self._response(
            auth,
            status,
            error,
            tool_calls=tool_calls,
        )

    def _response(
        self,
        auth: AgentAuthContext,
        status: str,
        reply: str,
        missing_fields: list[str] | None = None,
        available_rooms: list[dict] | None = None,
        draft: dict | None = None,
        tool_calls: list[dict] | None = None,
    ) -> ReservationAnalyzeResponse:
        return ReservationAnalyzeResponse(
            status=status,
            reply=reply,
            missingFields=missing_fields or [],
            availableRooms=available_rooms or [],
            draft=draft,
            toolCalls=[
                ToolCallSummary.model_validate(tool_call)
                for tool_call in (tool_calls or [])
            ],
            traceId=auth.claims.trace_id,
        )


@lru_cache
def get_reservation_analysis_service() -> ReservationAnalysisService:
    return ReservationAnalysisService(get_settings())
