"""Anthropic Claude API client wrapper."""
import anthropic
from app.config import settings

_client = anthropic.AsyncAnthropic(api_key=settings.anthropic_api_key)


async def get_llm_response(system_prompt: str, messages: list[dict]) -> str:
    response = await _client.messages.create(
        model=settings.llm_model,
        max_tokens=settings.llm_max_tokens,
        temperature=settings.llm_temperature,
        system=system_prompt,
        messages=messages,
    )
    return response.content[0].text
