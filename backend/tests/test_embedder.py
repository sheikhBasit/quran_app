"""TDD tests for the RAG embedder module.
Write these first — all should FAIL until embedder.py is implemented.
"""
import pytest


def test_embed_query_returns_list():
    from app.rag.embedder import embed_query
    result = embed_query("What does Islam say about prayer?")
    assert isinstance(result, list)


def test_embed_query_correct_dimension():
    from app.rag.embedder import embed_query
    assert len(embed_query("test")) == 384


def test_embed_passage_correct_dimension():
    from app.rag.embedder import embed_passage
    assert len(embed_passage("passage about prayer")) == 384


def test_embedding_is_unit_normalized():
    import numpy as np
    from app.rag.embedder import embed_query
    result = embed_query("test normalization")
    norm = np.linalg.norm(result)
    assert abs(norm - 1.0) < 1e-5


def test_similar_texts_closer_than_dissimilar():
    import numpy as np
    from app.rag.embedder import embed_query
    e1 = embed_query("prayer in Islam")
    e2 = embed_query("salah and worship")
    e3 = embed_query("cooking pasta recipe")
    assert np.dot(e1, e2) > np.dot(e1, e3)


def test_arabic_text_embeds_without_error():
    from app.rag.embedder import embed_passage
    result = embed_passage("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
    assert len(result) == 384


def test_query_and_passage_same_dimension():
    from app.rag.embedder import embed_query, embed_passage
    assert len(embed_query("test")) == len(embed_passage("test"))
