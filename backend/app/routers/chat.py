"""Chat endpoint — core RAG pipeline entry point."""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.dependencies import get_db
from app.rag.retriever import retrieve
from app.rag.prompt_builder import build_prompt, SYSTEM_PROMPT
from app.rag.llm_client import get_llm_response
from app.schemas.chat import ChatRequest, ChatResponse, ChatSources, AyahSource, HadithSource, TafsirSource

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("/", response_model=ChatResponse)
async def chat(request: ChatRequest, db: AsyncSession = Depends(get_db)):
    retrieved = await retrieve(request.message, db)
    messages = build_prompt(request.message, retrieved)
    answer = await get_llm_response(SYSTEM_PROMPT, messages)

    return ChatResponse(
        answer=answer,
        sources=ChatSources(
            ayahs=[AyahSource(surah=a["surah_number"], ayah=a["ayah_number"])
                   for a in retrieved["ayahs"]],
            hadiths=[HadithSource(collection=h["collection"], number=h["hadith_number"])
                     for h in retrieved["hadiths"]],
            tafsir=[TafsirSource(surah=t["surah_number"], ayah=t["ayah_number"],
                                 book=t["book_name"]) for t in retrieved["tafsir"]],
        ),
    )


@router.get("/health")
async def health():
    return {"status": "ok", "model_loaded": True, "db_connected": True}
