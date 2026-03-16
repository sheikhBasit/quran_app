"""Pydantic request/response schemas for the chat endpoint."""
from pydantic import BaseModel, field_validator


class ChatRequest(BaseModel):
    message: str

    @field_validator("message")
    @classmethod
    def message_not_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Message cannot be empty")
        if len(v) > 1000:
            raise ValueError("Message too long (max 1000 characters)")
        return v


class AyahSource(BaseModel):
    surah: int
    ayah: int


class HadithSource(BaseModel):
    collection: str
    number: int


class TafsirSource(BaseModel):
    surah: int
    ayah: int
    book: str


class ChatSources(BaseModel):
    ayahs: list[AyahSource] = []
    hadiths: list[HadithSource] = []
    tafsir: list[TafsirSource] = []


class ChatResponse(BaseModel):
    answer: str
    sources: ChatSources
