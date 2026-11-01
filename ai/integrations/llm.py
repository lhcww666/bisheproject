from langchain_core.language_models.chat_models import BaseChatModel
from langchain_openai import ChatOpenAI

from config.settings import Settings


class LlmUnavailableError(RuntimeError):
    """模型服务未配置。"""


def create_chat_model(settings: Settings) -> BaseChatModel:
    if not settings.llm_api_key:
        raise LlmUnavailableError("模型 API Key 未配置")
    return ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url or None,
        temperature=settings.llm_temperature,
        timeout=settings.llm_timeout_seconds,
        max_retries=1,
    )
