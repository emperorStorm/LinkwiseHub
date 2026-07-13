from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ai_service_token: str = ""
    mineru_base_url: str = "http://10.211.55.6:8000"
    mineru_api_token: str = ""
    mineru_bind_address: str = "10.211.55.6"
    mineru_port: int = 8000
    mineru_version: str = "3.4.4"
    mineru_image: str = "mineru-local:3.4.4"
    caddy_image: str = "caddy:2.10-alpine"
    mineru_model_source: str = "local"
    mineru_model_download_source: str = "auto"
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
            "MINERU_API_TOKEN": self.mineru_api_token,
            "MINIO_ACCESS_KEY": self.minio_access_key,
            "MINIO_SECRET_KEY": self.minio_secret_key,
        }
        return [name for name, value in required.items() if not value]


@lru_cache
def get_settings() -> Settings:
    return Settings()
