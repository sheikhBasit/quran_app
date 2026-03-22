"""TDD tests for the chat router endpoints."""
import pytest
from unittest.mock import patch, AsyncMock
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_chat_returns_200_valid_message():
    from app.main import app
    with patch("app.routers.chat.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.chat.get_llm_response", new_callable=AsyncMock) as mock_llm:
        mock_r.return_value = {"ayahs": [], "hadiths": [], "tafsir": []}
        mock_llm.return_value = "Zakat is the third pillar of Islam."

        async with AsyncClient(app=app, base_url="http://test") as client:
            response = await client.post("/chat/", json={"message": "What is Zakat?"})

        assert response.status_code == 200


def test_chat_response_has_required_fields():
    import asyncio
    from app.main import app
    with patch("app.routers.chat.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.chat.get_llm_response", new_callable=AsyncMock) as mock_llm:
        mock_r.return_value = {
            "ayahs": [{"surah_number": 9, "ayah_number": 60, "content": "Zakat is obligatory"}],
            "hadiths": [],
            "tafsir": [],
        }
        mock_llm.return_value = "Zakat is obligatory (Surah 9:60)."

        async def run():
            async with AsyncClient(app=app, base_url="http://test") as client:
                return await client.post("/chat/", json={"message": "What is Zakat?"})

        response = asyncio.get_event_loop().run_until_complete(run())
        data = response.json()
        assert "answer" in data
        assert "sources" in data
        assert "ayahs" in data["sources"]
        assert "hadiths" in data["sources"]


@pytest.mark.asyncio
async def test_chat_returns_400_for_empty_message():
    from app.main import app
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post("/chat/", json={"message": ""})
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_chat_returns_400_for_whitespace_only():
    from app.main import app
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post("/chat/", json={"message": "   "})
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_chat_returns_400_for_message_too_long():
    from app.main import app
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post("/chat/", json={"message": "x" * 1001})
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_health_endpoint_returns_ok():
    from app.main import app
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.get("/chat/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
