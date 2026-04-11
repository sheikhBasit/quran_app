"""Tests for the /chat/understand-ayah endpoint."""
from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

# ── Schema validation ─────────────────────────────────────────────────────────

def test_understand_request_valid():
    from app.schemas.understand import UnderstandRequest
    req = UnderstandRequest(
        surah=1, ayah=1,
        arabic_text="بِسْمِ اللَّهِ",
        translation="In the name of Allah",
    )
    assert req.surah == 1
    assert req.ayah == 1


def test_understand_request_rejects_surah_zero():
    from pydantic import ValidationError

    from app.schemas.understand import UnderstandRequest
    with pytest.raises(ValidationError):
        UnderstandRequest(surah=0, ayah=1, arabic_text="x", translation="y")


def test_understand_request_rejects_surah_115():
    from pydantic import ValidationError

    from app.schemas.understand import UnderstandRequest
    with pytest.raises(ValidationError):
        UnderstandRequest(surah=115, ayah=1, arabic_text="x", translation="y")


def test_understand_request_rejects_ayah_zero():
    from pydantic import ValidationError

    from app.schemas.understand import UnderstandRequest
    with pytest.raises(ValidationError):
        UnderstandRequest(surah=1, ayah=0, arabic_text="x", translation="y")


def test_understand_request_accepts_boundary_surahs():
    from app.schemas.understand import UnderstandRequest
    req1 = UnderstandRequest(surah=1, ayah=1, arabic_text="x", translation="y")
    req114 = UnderstandRequest(surah=114, ayah=1, arabic_text="x", translation="y")
    assert req1.surah == 1
    assert req114.surah == 114


# ── Prompt builder ────────────────────────────────────────────────────────────

def test_build_understand_prompt_returns_list():
    from app.rag.understand_prompt import build_understand_prompt
    messages = build_understand_prompt(
        surah=1, ayah=1,
        arabic_text="بِسْمِ اللَّهِ",
        translation="In the name of Allah",
        retrieved={"ayahs": [], "hadiths": [], "tafsir": []},
    )
    assert isinstance(messages, list)
    assert len(messages) >= 1


def test_build_understand_prompt_user_role():
    from app.rag.understand_prompt import build_understand_prompt
    messages = build_understand_prompt(
        surah=2, ayah=255,
        arabic_text="اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
        translation="Allah — there is no deity except Him",
        retrieved={"ayahs": [], "hadiths": [], "tafsir": []},
    )
    assert messages[-1]["role"] == "user"


def test_build_understand_prompt_contains_surah_ayah():
    from app.rag.understand_prompt import build_understand_prompt
    messages = build_understand_prompt(
        surah=36, ayah=1,
        arabic_text="يس",
        translation="Ya, Seen",
        retrieved={"ayahs": [], "hadiths": [], "tafsir": []},
    )
    content = messages[-1]["content"]
    assert "36" in content
    assert "1" in content


def test_build_understand_prompt_contains_arabic_and_translation():
    from app.rag.understand_prompt import build_understand_prompt
    arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ"
    translation = "Say: He is Allah, the One"
    messages = build_understand_prompt(
        surah=112, ayah=1,
        arabic_text=arabic,
        translation=translation,
        retrieved={"ayahs": [], "hadiths": [], "tafsir": []},
    )
    content = messages[-1]["content"]
    assert arabic in content
    assert translation in content


def test_system_prompt_has_four_sections():
    from app.rag.understand_prompt import UNDERSTAND_SYSTEM_PROMPT
    for section in ["CONTEXT", "WORD HIGHLIGHTS", "SCHOLAR VIEW", "PRACTICAL LESSON"]:
        assert section in UNDERSTAND_SYSTEM_PROMPT, f"Missing section: {section}"


def test_system_prompt_has_word_limit():
    from app.rag.understand_prompt import UNDERSTAND_SYSTEM_PROMPT
    # Should mention a word/token limit
    assert any(word in UNDERSTAND_SYSTEM_PROMPT for word in ["words", "tokens", "400", "concise", "brief"])


# ── HTTP endpoint ─────────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_understand_ayah_returns_200():
    from app.main import app

    async def fake_stream(*args, **kwargs):
        for token in ["## CONTEXT\n", "This is a test.", "\n## WORD HIGHLIGHTS\nWord."]:
            yield token

    with patch("app.routers.understand.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.understand.stream_llm_response", return_value=fake_stream()):
        mock_r.return_value = {"ayahs": [], "hadiths": [], "tafsir": []}

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/chat/understand-ayah",
                json={
                    "surah": 1, "ayah": 1,
                    "arabic_text": "بِسْمِ اللَّهِ",
                    "translation": "In the name of Allah",
                },
            )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_understand_ayah_returns_event_stream():
    from app.main import app

    async def fake_stream(*args, **kwargs):
        yield "token"

    with patch("app.routers.understand.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.understand.stream_llm_response", return_value=fake_stream()):
        mock_r.return_value = {"ayahs": [], "hadiths": [], "tafsir": []}

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/chat/understand-ayah",
                json={
                    "surah": 1, "ayah": 1,
                    "arabic_text": "بِسْمِ اللَّهِ",
                    "translation": "In the name of Allah",
                },
            )

    assert "text/event-stream" in response.headers.get("content-type", "")


@pytest.mark.asyncio
async def test_understand_ayah_streams_sse_format():
    from app.main import app

    tokens = ["Hello", " World"]

    async def fake_stream(*args, **kwargs):
        for t in tokens:
            yield t

    with patch("app.routers.understand.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.understand.stream_llm_response", return_value=fake_stream()):
        mock_r.return_value = {"ayahs": [], "hadiths": [], "tafsir": []}

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/chat/understand-ayah",
                json={
                    "surah": 2, "ayah": 255,
                    "arabic_text": "اللَّهُ لَا إِلَٰهَ",
                    "translation": "Allah, no deity but Him",
                },
            )

    # Each token should be wrapped in SSE data: prefix
    body = response.text
    assert "data: Hello" in body
    assert "data:  World" in body


@pytest.mark.asyncio
async def test_understand_ayah_rejects_invalid_surah():
    from app.main import app

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post(
            "/chat/understand-ayah",
            json={
                "surah": 200, "ayah": 1,
                "arabic_text": "x",
                "translation": "y",
            },
        )

    assert response.status_code == 400


@pytest.mark.asyncio
async def test_understand_ayah_calls_retrieve_with_query():
    from app.main import app

    async def fake_stream(*args, **kwargs):
        yield "token"

    with patch("app.routers.understand.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.understand.stream_llm_response", return_value=fake_stream()):
        mock_r.return_value = {"ayahs": [], "hadiths": [], "tafsir": []}

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            await client.post(
                "/chat/understand-ayah",
                json={
                    "surah": 1, "ayah": 1,
                    "arabic_text": "بِسْمِ اللَّهِ",
                    "translation": "In the name of Allah",
                },
            )

    # retrieve should have been called once with a non-empty query string
    mock_r.assert_called_once()
    query_arg = mock_r.call_args[0][0]
    assert "Surah 1" in query_arg
    assert "Ayah 1" in query_arg


@pytest.mark.asyncio
async def test_understand_ayah_error_yields_error_event():
    from app.main import app

    async def failing_stream(*args, **kwargs):
        raise RuntimeError("Groq timeout")
        yield  # make it an async generator

    with patch("app.routers.understand.retrieve", new_callable=AsyncMock) as mock_r, \
         patch("app.routers.understand.stream_llm_response", return_value=failing_stream()):
        mock_r.return_value = {"ayahs": [], "hadiths": [], "tafsir": []}

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post(
                "/chat/understand-ayah",
                json={
                    "surah": 1, "ayah": 1,
                    "arabic_text": "بِسْمِ اللَّهِ",
                    "translation": "In the name of Allah",
                },
            )

    # Error should be surfaced as a data: [ERROR] event, not a 500
    assert response.status_code == 200
    assert "[ERROR]" in response.text
