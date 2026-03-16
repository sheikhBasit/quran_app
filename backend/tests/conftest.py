import pytest
from unittest.mock import AsyncMock


@pytest.fixture
def fake_retrieved():
    return {
        "ayahs": [
            {
                "surah_number": 2,
                "ayah_number": 153,
                "content": "O believers, seek help through patience and prayer.",
            }
        ],
        "hadiths": [
            {
                "collection": "bukhari",
                "hadith_number": 1,
                "content": "Actions are judged by intentions. — Narrator: Umar ibn al-Khattab",
            }
        ],
        "tafsir": [
            {
                "surah_number": 2,
                "ayah_number": 153,
                "book_name": "ibn_kathir",
                "content": "This verse commands believers to seek help through patience and prayer.",
            }
        ],
    }


@pytest.fixture
def empty_retrieved():
    return {"ayahs": [], "hadiths": [], "tafsir": []}


@pytest.fixture
def mock_db():
    return AsyncMock()
