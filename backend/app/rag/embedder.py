"""Embedding model wrapper.
Uses intfloat/multilingual-e5-small (384 dimensions).
CRITICAL: Always prefix query: for queries, passage: for documents.
"""
from functools import lru_cache
from sentence_transformers import SentenceTransformer

MODEL_NAME = "intfloat/multilingual-e5-small"


@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    """Singleton — load once, reuse across all requests."""
    return SentenceTransformer(MODEL_NAME)


def embed_query(text: str) -> list[float]:
    """Embed a user query. Prefix 'query: ' per e5 model convention."""
    return get_model().encode(f"query: {text}", normalize_embeddings=True).tolist()


def embed_passage(text: str) -> list[float]:
    """Embed a document passage. Prefix 'passage: ' per e5 model convention."""
    return get_model().encode(f"passage: {text}", normalize_embeddings=True).tolist()
