#!/usr/bin/env python3
"""Ingest Tafsir books into pgvector.
Real data: data/tafsir/{book}.json  (single flat file)
Fields: surah_number, ayat_number, tafseer
"""
import json, asyncio
from pathlib import Path
import asyncpg
from app.rag.embedder import embed_passage
from app.config import settings

TAFSIR_BOOKS = ["ibn_kathir", "maarif", "ibn_abbas"]
BATCH_SIZE = 50


def load_tafsir(book_name: str) -> list:
    """Load tafsir entries from single .json file or folder."""
    json_file = Path(f"data/tafsir/{book_name}.json")
    folder    = Path(f"data/tafsir/{book_name}")
    if json_file.exists():
        with open(json_file, encoding="utf-8") as f:
            entries = json.load(f)
        return entries if isinstance(entries, list) else [entries]
    elif folder.exists():
        entries = []
        for f in sorted(folder.glob("*.json")):
            with open(f, encoding="utf-8") as fp:
                data = json.load(fp)
                entries.extend(data if isinstance(data, list) else [data])
        return entries
    return []


def get_content(e: dict) -> str:
    """Get tafsir text — real field is 'tafseer', fallback to 'content'."""
    return (e.get("tafseer") or e.get("content") or "").strip()


def get_ayah_num(e: dict):
    """Get ayah number — real field is 'ayat_number', fallback to 'ayah_number'."""
    return e.get("ayat_number") or e.get("ayah_number")


async def ingest():
    conn = await asyncpg.connect(settings.database_url.replace("+asyncpg", ""))

    for book_name in TAFSIR_BOOKS:
        entries = load_tafsir(book_name)
        if not entries:
            print(f"⚠  Skipping {book_name} — no entries found")
            continue

        batch, total = [], 0

        for entry in entries:
            content  = get_content(entry)
            ayah_num = get_ayah_num(entry)
            surah    = entry.get("surah_number")
            if not content or not ayah_num or not surah:
                continue
            embedding = embed_passage(content)
            batch.append((surah, ayah_num, book_name, content, embedding))

            if len(batch) >= BATCH_SIZE:
                await conn.executemany(
                    "INSERT INTO tafsir_embeddings"
                    "(surah_number,ayah_number,book_name,content,embedding) "
                    "VALUES($1,$2,$3,$4,$5::vector) ON CONFLICT DO NOTHING",
                    batch
                )
                total += len(batch)
                print(f"  [{book_name}] {total}/{len(entries)}...", end="\r")
                batch = []

        if batch:
            await conn.executemany(
                "INSERT INTO tafsir_embeddings"
                "(surah_number,ayah_number,book_name,content,embedding) "
                "VALUES($1,$2,$3,$4,$5::vector) ON CONFLICT DO NOTHING",
                batch
            )
            total += len(batch)

        print(f"\n✅ Tafsir [{book_name}]: {total} entries ingested")

    await conn.close()


if __name__ == "__main__":
    asyncio.run(ingest())
