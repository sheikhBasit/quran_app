"""FastAPI application entry point."""
from fastapi import FastAPI
from app.routers import chat

app = FastAPI(title="Quran App API", version="1.0.0")
app.include_router(chat.router)
