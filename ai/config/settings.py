from functools import lru_cache

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    agent_context_secret: str = Field(
        default="",
        validation_alias=AliasChoices(
            "AGENT_CONTEXT_SECRET",
            "AI_AGENT_CONTEXT_SECRET",
        ),
    )
    llm_api_key: str = Field(
        default="",
        validation_alias=AliasChoices("AI_LLM_API_KEY", "DEEPSEEK_API_KEY"),
    )
    llm_base_url: str = Field(
        default="https://api.deepseek.com",
        validation_alias=AliasChoices("AI_LLM_BASE_URL", "DEEPSEEK_BASE_URL"),
    )
    llm_model: str = Field(
        default="deepseek-chat",
        validation_alias=AliasChoices("AI_LLM_MODEL", "DEEPSEEK_MODEL"),
    )
    llm_temperature: float = Field(
        default=0.2,
        validation_alias=AliasChoices("AI_LLM_TEMPERATURE"),
    )
    llm_timeout_seconds: float = Field(
        default=45.0,
        validation_alias=AliasChoices("AI_LLM_TIMEOUT_SECONDS"),
    )
    java_base_url: str = Field(
        default="http://127.0.0.1:8087",
        validation_alias=AliasChoices("AI_JAVA_BASE_URL"),
    )
    timezone_offset_hours: float = Field(
        default=8.0,
        validation_alias=AliasChoices("AI_TIMEZONE_OFFSET_HOURS"),
    )
    model_config = SettingsConfigDict(
        env_prefix="AI_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True,
    )

    app_name: str = "校园实验室 Python Agent 服务"
    app_version: str = "0.1.0"
    environment: str = "local"
    debug: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
