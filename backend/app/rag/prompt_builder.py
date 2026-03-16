"""RAG prompt builder — formats retrieved chunks into LLM prompt."""

SYSTEM_PROMPT = """You are an Islamic knowledge assistant.
Answer using ONLY the provided references. Rules:
1. Cite every claim: (Surah X:Y) for Ayahs, (Collection #N) for Hadith
2. If no reference answers the question — say so explicitly. NEVER fabricate.
3. Present multiple Tafsir perspectives when Ibn Kathir, Maarif, and Ibn Abbas differ
4. Keep language clear and accessible for learners and non-Muslims
5. End EVERY response with exactly:
   "For personal guidance, please consult a qualified Islamic scholar."
"""


def build_prompt(query: str, retrieved: dict) -> list[dict]:
    parts: list[str] = []

    if retrieved["ayahs"]:
        parts.append("=== QURAN VERSES ===")
        for a in retrieved["ayahs"]:
            parts.append(f"[Surah {a['surah_number']}:{a['ayah_number']}]\n{a['content']}")

    if retrieved["hadiths"]:
        parts.append("\n=== HADITH ===")
        for h in retrieved["hadiths"]:
            col = h["collection"].title()
            parts.append(f"[{col} #{h['hadith_number']}]\n{h['content']}")

    if retrieved["tafsir"]:
        parts.append("\n=== TAFSIR ===")
        for t in retrieved["tafsir"]:
            book = t["book_name"].replace("_", " ").title()
            parts.append(
                f"[{book} on {t['surah_number']}:{t['ayah_number']}]\n{t['content']}"
            )

    if not parts:
        parts.append("No relevant references found for this query.")

    context = "\n".join(parts)
    return [{"role": "user", "content": f"References:\n{context}\n\nQuestion: {query}"}]
