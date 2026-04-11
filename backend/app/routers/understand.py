"""Understand-ayah endpoint — teaching mode for the Quran learning system."""
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.dependencies import get_db
from app.rag.llm_client import stream_llm_response
from app.rag.retriever import retrieve
from app.rag.understand_prompt import UNDERSTAND_SYSTEM_PROMPT, build_understand_prompt
from app.schemas.understand import UnderstandRequest

router = APIRouter(prefix="/chat", tags=["understand"])


@router.post("/understand-ayah")
async def understand_ayah(request: UnderstandRequest, db: AsyncSession = Depends(get_db)):
    """Stream a structured 4-section teaching explanation for a single ayah."""
    query = f"Explain Surah {request.surah} Ayah {request.ayah}: {request.translation}"
    retrieved = await retrieve(query, db)
    messages = build_understand_prompt(
        surah=request.surah,
        ayah=request.ayah,
        arabic_text=request.arabic_text,
        translation=request.translation,
        retrieved=retrieved,
    )

    async def event_generator():
        try:
            async for token in stream_llm_response(
                UNDERSTAND_SYSTEM_PROMPT, messages, history=[]
            ):
                yield f"data: {token}\n\n"
        except Exception as e:
            yield f"data: [ERROR] {str(e)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
