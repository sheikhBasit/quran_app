"""Embedding model wrapper.
Uses intfloat/multilingual-e5-small (384 dimensions).
CRITICAL: Always prefix query: for queries, passage: for documents.
"""
from functools import lru_cache
from sentence_transformers import SentenceTransformer, CrossEncoder
from app.config import settings

MODEL_NAME = "intfloat/multilingual-e5-small"


@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    """Singleton — load once, reuse across all requests."""
    return SentenceTransformer(MODEL_NAME)


@lru_cache(maxsize=1)
def get_reranker() -> CrossEncoder:
    """Singleton — load Cross-Encoder once."""
    return CrossEncoder(settings.rerank_model_name)


def embed_query(text: str) -> list[float]:
    """Embed a user query. Prefix 'query: ' per e5 model convention."""
    return get_model().encode(f"query: {text}", normalize_embeddings=True).tolist()


def embed_passage(text: str) -> list[float]:
    """Embed a single passage. Prefix 'passage: ' per e5 model convention."""
    return get_model().encode(f"passage: {text}", normalize_embeddings=True).tolist()


def embed_passages(texts: list[str]) -> list[list[float]]:
    """Embed multiple passages in batch. Prefix each with 'passage: '."""
    prefixed = [f"passage: {t}" for t in texts]
    embeddings = get_model().encode(
        prefixed, 
        batch_size=len(texts), 
        normalize_embeddings=True,
        show_progress_bar=False
    )
    return embeddings.tolist()


def compute_rerank_scores(query: str, passages: list[str]) -> list[float]:
    """Compute relevance scores for a list of passages against a query."""
    if not passages: return []
    cross_encoder = get_reranker()
    pairs = [[query, p] for p in passages]
    scores = cross_encoder.predict(pairs)
    return scores.tolist()
