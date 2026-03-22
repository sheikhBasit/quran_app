"""pgvector similarity retriever.
Runs 3 parallel cosine similarity queries against PostgreSQL+pgvector.
"""
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.rag.embedder import embed_query, compute_rerank_scores
from app.rag.prompt_builder import HYDE_PROMPT
from app.rag.llm_client import get_llm_response
from app.config import settings

MIN_SIM = settings.min_similarity
TOP_K = settings.top_k
RERANK_TOP_K = settings.rerank_top_k


async def retrieve(query: str, db: AsyncSession) -> dict:
    """Retrieve relevant chunks using HyDE and Cross-Encoder Reranking."""
    search_query = query

    # 1. HyDE: Generate hypothetical answer
    if settings.hyde_enabled:
        try:
            hypothetical_answer = await get_llm_response(
                HYDE_PROMPT, [{"role": "user", "content": query}]
            )
            print(f"DEBUG HyDE answer: {hypothetical_answer[:100]}...")
            search_query = hypothetical_answer
        except Exception as e:
            print(f"DEBUG HyDE failed (falling back): {e}")

    # 2. Vector Retrieval
    vec = embed_query(search_query)
    emb_str = f"[{','.join(map(str, vec))}]"
    params = {"emb": emb_str, "min_sim": MIN_SIM, "top_k": RERANK_TOP_K}

    # Parallel retrieval from 3 sources
    ayahs_result = await db.execute(text("""
        SELECT surah_number, ayah_number, content, 1 - (embedding <=> CAST(:emb AS vector)) AS sim
        FROM ayah_embeddings WHERE 1 - (embedding <=> CAST(:emb AS vector)) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), params)

    hadiths_result = await db.execute(text("""
        SELECT collection, hadith_number, content, 1 - (embedding <=> CAST(:emb AS vector)) AS sim
        FROM hadith_embeddings WHERE 1 - (embedding <=> CAST(:emb AS vector)) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), params)

    tafsir_result = await db.execute(text("""
        SELECT surah_number, ayah_number, book_name, content, 1 - (embedding <=> CAST(:emb AS vector)) AS sim
        FROM tafsir_embeddings WHERE 1 - (embedding <=> CAST(:emb AS vector)) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), params)

    # 3. Combine and Rerank
    candidates = []
    for r in ayahs_result:
        d = dict(r._mapping)
        d["type"] = "ayah"
        candidates.append(d)
    for r in hadiths_result:
        d = dict(r._mapping)
        d["type"] = "hadith"
        candidates.append(d)
    for r in tafsir_result:
        d = dict(r._mapping)
        d["type"] = "tafsir"
        candidates.append(d)

    if not candidates:
        return {"ayahs": [], "hadiths": [], "tafsir": []}

    # Cross-Encoder Reranking
    if settings.rerank_enabled:
        passages = [c["content"] for c in candidates]
        scores = compute_rerank_scores(query, passages)
        for i, score in enumerate(scores):
            candidates[i]["rerank_score"] = score
        
        candidates.sort(key=lambda x: x.get("rerank_score", 0), reverse=True)
        # Log top 3 scores
        top_3_scores = [round(c.get("rerank_score", 0), 4) for c in candidates[:3]]
        print(f"DEBUG Rerank top 3 scores: {top_3_scores}")

    # Final selection: Top-K (7)
    final_docs = candidates[:TOP_K]

    return {
        "ayahs":   [d for d in final_docs if d["type"] == "ayah"],
        "hadiths": [d for d in final_docs if d["type"] == "hadith"],
        "tafsir":  [d for d in final_docs if d["type"] == "tafsir"],
    }
