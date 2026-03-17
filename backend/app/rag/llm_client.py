"""Groq API client — free tier, fast inference, no credit card needed.

Sign up free: https://console.groq.com
Get API key: https://console.groq.com/keys
Free tier: 14,400 requests/day, 6,000 tokens/minute

Model: llama-3.3-70b-versatile — excellent for Islamic Q&A
"""
import httpx
from app.config import settings


async def get_llm_response(system_prompt: str, messages: list[dict]) -> str:
    """Call Groq API — OpenAI-compatible chat format."""
    payload = {
        "model": settings.llm_model,
        "messages": [{"role": "system", "content": system_prompt}] + messages,
        "temperature": settings.llm_temperature,
        "max_tokens": settings.llm_max_tokens,
    }
    headers = {
        "Authorization": f"Bearer {settings.groq_api_key}",
        "Content-Type": "application/json",
    }
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.post(
            "https://api.groq.com/openai/v1/chat/completions",
            json=payload,
            headers=headers,
        )
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]
