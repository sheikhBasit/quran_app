"""Groq API client — free tier, fast inference, no credit card needed.

Sign up free: https://console.groq.com
Get API key: https://console.groq.com/keys
Free tier: 14,400 requests/day, 6,000 tokens/minute

Model: llama-3.3-70b-versatile — excellent for Islamic Q&A
"""
import httpx
from app.config import settings


async def get_llm_response(system_prompt: str, messages: list[dict], history: list[dict] = []) -> str:
    """Call Groq API — OpenAI-compatible chat format."""
    truncated_history = history[-4:] if history else []
    payload = {
        "model": settings.llm_model,
        "messages": [{"role": "system", "content": system_prompt}] + truncated_history + messages,
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


async def stream_llm_response(system_prompt: str, messages: list[dict], history: list[dict] = []):
    """Stream response from Groq API."""
    truncated_history = history[-4:] if history else []
    payload = {
        "model": settings.llm_model,
        "messages": [{"role": "system", "content": system_prompt}] + truncated_history + messages,
        "temperature": settings.llm_temperature,
        "max_tokens": settings.llm_max_tokens,
        "stream": True,
    }
    headers = {
        "Authorization": f"Bearer {settings.groq_api_key}",
        "Content-Type": "application/json",
    }
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        async with client.stream(
            "POST",
            "https://api.groq.com/openai/v1/chat/completions",
            json=payload,
            headers=headers,
        ) as response:
            response.raise_for_status()
            async for line in response.aiter_lines():
                if not line.startswith("data: "):
                    continue
                if line == "data: [DONE]":
                    break
                
                import json
                try:
                    data = json.loads(line[6:])
                    delta = data["choices"][0].get("delta", {})
                    if "content" in delta:
                        yield delta["content"]
                except Exception:
                    continue
