"""pgvector similarity retriever.
Runs 3 parallel cosine similarity queries against PostgreSQL+pgvector.
"""
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.rag.embedder import embed_query
from app.config import settings

MIN_SIM = settings.min_similarity
TOP_K = settings.top_k


async def retrieve(query: str, db: AsyncSession) -> dict:
    """Retrieve top-k relevant chunks from all three sources."""
    vec = embed_query(query)
    emb_str = f"[{','.join(map(str, vec))}]"
    params = {"emb": emb_str, "min_sim": MIN_SIM, "top_k": TOP_K}

    ayahs_result = await db.execute(text("""
        SELECT surah_number, ayah_number, content,
               1 - (embedding <=> :emb::vector) AS sim
        FROM ayah_embeddings
        WHERE 1 - (embedding <=> :emb::vector) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), params)

    hadiths_result = await db.execute(text("""
        SELECT collection, hadith_number, content,
               1 - (embedding <=> :emb::vector) AS sim
        FROM hadith_embeddings
        WHERE 1 - (embedding <=> :emb::vector) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), params)

    tafsir_result = await db.execute(text("""
        SELECT surah_number, ayah_number, book_name, content,
               1 - (embedding <=> :emb::vector) AS sim
        FROM tafsir_embeddings
        WHERE 1 - (embedding <=> :emb::vector) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), params)

    return {
        "ayahs":   [dict(r._mapping) for r in ayahs_result],
        "hadiths": [dict(r._mapping) for r in hadiths_result],
        "tafsir":  [dict(r._mapping) for r in tafsir_result],
    }
