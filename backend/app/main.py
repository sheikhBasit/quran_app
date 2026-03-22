"""FastAPI application entry point."""
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from app.routers import chat

app = FastAPI(title="Quran App API", version="1.0.0")

@app.get("/health")
async def health():
    return {"status": "ok", "service": "quran-app-api"}

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    # Convert errors to a list of strings to ensure JSON serializability
    errors = [str(error) for error in exc.errors()]
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={"detail": errors},
    )

app.include_router(chat.router)
