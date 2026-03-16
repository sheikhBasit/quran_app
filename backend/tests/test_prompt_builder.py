"""TDD tests for the RAG prompt builder."""
from app.rag.prompt_builder import build_prompt, SYSTEM_PROMPT


def _r(ayahs=None, hadiths=None, tafsir=None):
    return {"ayahs": ayahs or [], "hadiths": hadiths or [], "tafsir": tafsir or []}


def test_prompt_includes_ayah_reference():
    retrieved = _r(ayahs=[{"surah_number": 2, "ayah_number": 153,
                            "content": "Seek help through patience"}])
    messages = build_prompt("patience", retrieved)
    assert any("2:153" in m["content"] for m in messages)


def test_prompt_includes_hadith_reference():
    retrieved = _r(hadiths=[{"collection": "bukhari", "hadith_number": 1,
                              "content": "Actions by intentions"}])
    messages = build_prompt("intentions", retrieved)
    assert any("Bukhari" in m["content"] for m in messages)


def test_prompt_includes_tafsir_reference():
    retrieved = _r(tafsir=[{"surah_number": 1, "ayah_number": 1,
                             "book_name": "ibn_kathir", "content": "Commentary"}])
    messages = build_prompt("fatiha", retrieved)
    assert any("ibn_kathir" in m["content"].lower() or "ibn kathir" in m["content"].lower()
               for m in messages)


def test_prompt_handles_empty_retrieval():
    messages = build_prompt("test query", _r())
    assert any("No relevant" in m["content"] for m in messages)


def test_prompt_returns_list_of_dicts():
    messages = build_prompt("test", _r())
    assert isinstance(messages, list)
    assert all("role" in m and "content" in m for m in messages)


def test_system_prompt_requires_citations():
    assert "Cite every claim" in SYSTEM_PROMPT


def test_system_prompt_has_scholar_disclaimer():
    assert "qualified Islamic scholar" in SYSTEM_PROMPT


def test_system_prompt_forbids_fabrication():
    assert "NEVER fabricate" in SYSTEM_PROMPT or "never fabricate" in SYSTEM_PROMPT.lower()
