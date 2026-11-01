from pydantic import BaseModel, Field


class ReservationExtraction(BaseModel):
    start_time: str | None = Field(
        default=None,
        description="开始时间，格式 yyyy-MM-dd HH:mm:ss",
    )
    end_time: str | None = Field(
        default=None,
        description="结束时间，格式 yyyy-MM-dd HH:mm:ss",
    )
    participant_count: int | None = Field(
        default=None,
        ge=1,
        description="参与人数",
    )
    experiment_name: str | None = Field(
        default=None,
        description="实验名称或实验内容",
    )
    room_type: str | None = Field(
        default=None,
        description="实验室类型偏好",
    )
    room_name: str | None = Field(
        default=None,
        description="用户明确选择的实验室名称或编号",
    )
    device_names: list[str] = Field(
        default_factory=list,
        description="用户需要的设备名称",
    )
