"""Request schema for the understand-ayah endpoint."""
from pydantic import BaseModel, field_validator


class UnderstandRequest(BaseModel):
    surah: int
    ayah: int
    arabic_text: str
    translation: str

    @field_validator("surah")
    @classmethod
    def valid_surah(cls, v: int) -> int:
        if not 1 <= v <= 114:
            raise ValueError("surah must be between 1 and 114")
        return v

    @field_validator("ayah")
    @classmethod
    def valid_ayah(cls, v: int) -> int:
        if v < 1:
            raise ValueError("ayah must be >= 1")
        return v
