"""App configuration via environment variables."""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str = "postgresql+asyncpg://quranapp:password@localhost:5432/qurandb"
    anthropic_api_key: str = ""
    log_level: str = "info"
    workers: int = 2
    min_similarity: float = 0.30
    top_k: int = 5
    llm_model: str = "claude-sonnet-4-20250514"
    llm_temperature: float = 0.2
    llm_max_tokens: int = 1000

    class Config:
        env_file = ".env"


settings = Settings()
