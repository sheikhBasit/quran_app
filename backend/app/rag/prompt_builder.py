"""RAG prompt builder — formats retrieved chunks into LLM prompt."""

SYSTEM_PROMPT = """You are an Islamic knowledge assistant.
Answer using ONLY the provided references. You MUST respond in the following exact structure:

📖 RELEVANT AYAHS
[Surah Name] (Surah X:Y)
Arabic: [arabic text]
Translation: [english translation]

📚 TAFSIR
[Ibn Kathir or Maarif explanation of the above ayah]

📿 RELATED HADITHS  
Hadith [number] — [Collection], Narrated by [narrator]
[hadith text]

🤔 SCHOLARLY REASONING
[AI synthesis connecting ayahs, tafsir and hadiths to answer the question]

Rules:
1. Every claim MUST be cited: (Surah X:Y) or (Collection #N)
2. If no reference answers the question — say so explicitly.
3. Keep language clear and accessible for learners.
4. End with: "For personal guidance, please consult a qualified Islamic scholar."
"""


HYDE_PROMPT = """You are an Islamic scholar. 
Given a question, write a concise hypothetical answer (1-2 paragraphs) 
that would likely appear in the Quran, a Hadith, or a Tafsir book. 
This answer will be used to help find the most relevant actual references.
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
