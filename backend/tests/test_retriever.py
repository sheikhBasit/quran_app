"""TDD tests for the RAG retriever module."""
import pytest
from unittest.mock import AsyncMock, MagicMock


@pytest.mark.asyncio
async def test_retrieve_returns_correct_keys(mock_db):
    from app.rag.retriever import retrieve
    mock_result = MagicMock()
    mock_result.__iter__ = MagicMock(return_value=iter([]))
    mock_db.execute.return_value = mock_result

    result = await retrieve("prayer", mock_db)
    assert set(result.keys()) == {"ayahs", "hadiths", "tafsir"}


@pytest.mark.asyncio
async def test_retrieve_queries_all_three_tables(mock_db):
    from app.rag.retriever import retrieve
    mock_result = MagicMock()
    mock_result.__iter__ = MagicMock(return_value=iter([]))
    mock_db.execute.return_value = mock_result

    await retrieve("what is zakat", mock_db)
    assert mock_db.execute.call_count == 3


@pytest.mark.asyncio
async def test_retrieve_returns_lists(mock_db):
    from app.rag.retriever import retrieve
    mock_result = MagicMock()
    mock_result.__iter__ = MagicMock(return_value=iter([]))
    mock_db.execute.return_value = mock_result

    result = await retrieve("test", mock_db)
    assert isinstance(result["ayahs"], list)
    assert isinstance(result["hadiths"], list)
    assert isinstance(result["tafsir"], list)


@pytest.mark.asyncio
async def test_retrieve_empty_when_db_returns_nothing(mock_db):
    from app.rag.retriever import retrieve
    mock_result = MagicMock()
    mock_result.__iter__ = MagicMock(return_value=iter([]))
    mock_db.execute.return_value = mock_result

    result = await retrieve("xyznonexistentquery", mock_db)
    assert result == {"ayahs": [], "hadiths": [], "tafsir": []}
