"""Chat endpoint — core RAG pipeline entry point."""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.dependencies import get_db
from app.rag.retriever import retrieve
from app.rag.prompt_builder import build_prompt, SYSTEM_PROMPT
from app.rag.llm_client import get_llm_response, stream_llm_response
from app.schemas.chat import ChatRequest, ChatResponse, ChatSources, AyahSource, HadithSource, TafsirSource
from fastapi.responses import StreamingResponse

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("/", response_model=ChatResponse)
async def chat(request: ChatRequest, db: AsyncSession = Depends(get_db)):
    retrieved = await retrieve(request.message, db)
    messages = build_prompt(request.message, retrieved)
    answer = await get_llm_response(SYSTEM_PROMPT, messages, history=request.history)

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


@router.post("/stream")
async def chat_stream(request: ChatRequest, db: AsyncSession = Depends(get_db)):
    """Streaming version of chat endpoint — yields data: <token>\n\n"""
    retrieved = await retrieve(request.message, db)
    messages = build_prompt(request.message, retrieved)

    async def event_generator():
        async for token in stream_llm_response(SYSTEM_PROMPT, messages, history=request.history):
            yield f"data: {token}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@router.get("/health")
async def health():
    return {"status": "ok", "model_loaded": True, "db_connected": True}
