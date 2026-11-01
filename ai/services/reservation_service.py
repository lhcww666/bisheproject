import json
from collections.abc import Callable
from datetime import datetime, timedelta, timezone

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage

from config.settings import Settings
from integrations.llm import create_chat_model
from schemas.reservation import ReservationExtraction

REQUIRED_FIELD_LABELS = {
    "start_time": "开始时间",
    "end_time": "结束时间",
    "participant_count": "参与人数",
}


class ReservationExtractionError(ValueError):
    """预约参数抽取失败。"""


class ReservationExtractionService:
    def __init__(
        self,
        settings: Settings,
        model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
    ) -> None:
        self.settings = settings
        self.model_factory = model_factory

    async def extract(
        self,
        message: str,
    ) -> tuple[ReservationExtraction, list[str]]:
        current_time = datetime.now(
            timezone(timedelta(hours=self.settings.timezone_offset_hours))
        ).strftime("%Y-%m-%d %H:%M:%S")
        prompt = (
            "你是预约参数抽取器。只输出 JSON，不要输出 Markdown。\n"
            f"当前时间：{current_time}，时区 UTC+{self.settings.timezone_offset_hours:g}。\n"
            "把用户请求解析为以下字段：\n"
            "start_time, end_time, participant_count, experiment_name, room_type, device_names。\n"
            "时间必须转换为 yyyy-MM-dd HH:mm:ss。无法确定的字段用 null。"
        )
        result = await self.model_factory(self.settings).ainvoke(
            [
                SystemMessage(content=prompt),
                HumanMessage(content=message),
            ]
        )
        text = result.content if isinstance(result.content, str) else str(result.content)
        try:
            payload = _extract_json(text)
            extraction = ReservationExtraction.model_validate(payload)
        except Exception as exception:
            raise ReservationExtractionError("预约参数解析失败") from exception

        extraction.start_time = _normalize_datetime(extraction.start_time)
        extraction.end_time = _normalize_datetime(extraction.end_time)
        missing = [
            field
            for field in ("start_time", "end_time", "participant_count")
            if getattr(extraction, field) is None
        ]
        if extraction.start_time and extraction.end_time:
            start = datetime.strptime(extraction.start_time, "%Y-%m-%d %H:%M:%S")
            end = datetime.strptime(extraction.end_time, "%Y-%m-%d %H:%M:%S")
            if end <= start:
                missing.append("time_range")
        return extraction, missing


def missing_field_message(fields: list[str]) -> str:
    labels = [REQUIRED_FIELD_LABELS.get(field, "有效时间范围") for field in fields]
    return f"请补充以下预约信息：{'、'.join(labels)}。"


def _extract_json(text: str) -> dict:
    start = text.find("{")
    end = text.rfind("}")
    if start < 0 or end <= start:
        raise ValueError("JSON object not found")
    return json.loads(text[start : end + 1])


def _normalize_datetime(value: str | None) -> str | None:
    if not value:
        return None
    for pattern in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"):
        try:
            return datetime.strptime(value, pattern).strftime("%Y-%m-%d %H:%M:%S")
        except ValueError:
            pass
    try:
        return datetime.fromisoformat(value).strftime("%Y-%m-%d %H:%M:%S")
    except ValueError:
        return None
