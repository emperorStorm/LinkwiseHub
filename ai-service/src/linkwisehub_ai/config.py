from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ai_service_token: str = ""
    mineru_base_url: str = "http://127.0.0.1:8000"
    mineru_request_timeout_seconds: float = 120
    mineru_result_timeout_seconds: float = 300
    minio_endpoint: str = "127.0.0.1:9000"
    minio_access_key: str = ""
    minio_secret_key: str = ""
    minio_secure: bool = False
    minio_result_bucket: str = "knowledge-base"

    def missing_required_values(self) -> list[str]:
        required = {
            "AI_SERVICE_TOKEN": self.ai_service_token,
            "MINIO_ACCESS_KEY": self.minio_access_key,
            "MINIO_SECRET_KEY": self.minio_secret_key,
        }
        return [name for name, value in required.items() if not value]


@lru_cache
def get_settings() -> Settings:
    return Settings()
