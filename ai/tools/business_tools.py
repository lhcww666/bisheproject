from pydantic import BaseModel, Field
from langchain_core.tools import StructuredTool

from integrations.java_api import JavaApiClient
from schemas.agent import AgentAuthContext


class ReservationQueryInput(BaseModel):
    status: str | None = Field(
        default=None,
        description="可选预约状态，例如 0 待审核、1 已通过、2 已驳回",
    )


class AvailableRoomQueryInput(BaseModel):
    start_time: str = Field(description="开始时间，格式 yyyy-MM-dd HH:mm:ss")
    end_time: str = Field(description="结束时间，格式 yyyy-MM-dd HH:mm:ss")
    participant_count: int | None = Field(
        default=None,
        ge=1,
        description="参与人数",
    )


class AvailableDeviceQueryInput(BaseModel):
    room_id: int = Field(gt=0, description="实验室ID")


class BusinessToolService:
    def __init__(self, java_api_client: JavaApiClient) -> None:
        self.java_api_client = java_api_client

    def langchain_tools(self, auth: AgentAuthContext) -> list[StructuredTool]:
        async def query_my_reservations(
            status: str | None = None,
        ) -> dict:
            return await self.java_api_client.get_my_reservations(
                auth.token,
                auth.request_id,
                status=status,
            )

        async def check_safety_exam_status() -> dict:
            return await self.java_api_client.check_safety_exam_status(
                auth.token,
                auth.request_id,
            )

        async def query_my_credit() -> dict:
            return await self.java_api_client.query_my_credit(
                auth.token,
                auth.request_id,
            )

        async def query_available_rooms(
            start_time: str,
            end_time: str,
            participant_count: int | None = None,
        ) -> dict:
            return await self.java_api_client.query_available_rooms(
                auth.token,
                auth.request_id,
                start_time,
                end_time,
                participant_count,
            )

        async def query_available_devices(room_id: int) -> dict:
            return await self.java_api_client.query_available_devices(
                auth.token,
                auth.request_id,
                room_id,
            )

        return [
            StructuredTool.from_function(
                coroutine=query_my_reservations,
                name="queryMyReservations",
                description="查询当前登录用户自己的预约记录，不返回其他用户数据。",
                args_schema=ReservationQueryInput,
            ),
            StructuredTool.from_function(
                coroutine=check_safety_exam_status,
                name="checkSafetyExamStatus",
                description="查询当前登录用户的安全考试通过状态和有效期。",
            ),
            StructuredTool.from_function(
                coroutine=query_my_credit,
                name="queryMyCredit",
                description="查询当前登录用户的信用分和是否允许预约。",
            ),
            StructuredTool.from_function(
                coroutine=query_available_rooms,
                name="queryAvailableRooms",
                description="查询指定时段、人数条件下当前用户可预约的实验室。",
                args_schema=AvailableRoomQueryInput,
            ),
            StructuredTool.from_function(
                coroutine=query_available_devices,
                name="queryAvailableDevices",
                description="查询指定实验室当前可预约的设备。",
                args_schema=AvailableDeviceQueryInput,
            ),
        ]

    async def invoke_tool(
        self,
        auth: AgentAuthContext,
        tool_name: str,
        arguments: dict | None = None,
    ) -> dict:
        tools = {tool.name: tool for tool in self.langchain_tools(auth)}
        if tool_name not in tools:
            raise ValueError(f"Unknown business tool: {tool_name}")
        result = await tools[tool_name].ainvoke(arguments or {})
        return result if isinstance(result, dict) else {"content": str(result)}
