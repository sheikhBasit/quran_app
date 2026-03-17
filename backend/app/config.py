"""App configuration via environment variables."""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Database
    database_url: str = "postgresql+asyncpg://quranapp:quranapp123@localhost:5432/qurandb"

    # Groq API (free tier — sign up at console.groq.com)
    groq_api_key: str = ""
    llm_model: str = "llama-3.3-70b-versatile"
    llm_temperature: float = 0.2
    llm_max_tokens: int = 1000

    # RAG retrieval settings
    min_similarity: float = 0.30
    top_k: int = 5

    # Server
    log_level: str = "info"
    workers: int = 2

    class Config:
        env_file = ".env"


settings = Settings()
