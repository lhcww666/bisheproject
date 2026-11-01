from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    secret_key: str = ""
    model_config = SettingsConfigDict(
        env_prefix="AI_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "校园实验室 Python Agent 服务"
    app_version: str = "0.1.0"
    environment: str = "local"
    debug: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
