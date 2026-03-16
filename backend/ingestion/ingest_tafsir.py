#!/usr/bin/env python3
"""Ingest Tafsir books into pgvector.
Ingests: Ibn Kathir, Maarif ul Quran, Ibn Abbas (English).
"""
import json, asyncio
from pathlib import Path
import asyncpg
from app.rag.embedder import embed_passage
from app.config import settings

TAFSIR_BOOKS = {
    "ibn_kathir": "data/tafsir/ibn_kathir",
    "maarif":     "data/tafsir/maarif",
    "ibn_abbas":  "data/tafsir/ibn_abbas",
}
BATCH_SIZE = 50


def load_folder(folder: str) -> list:
    entries = []
    for f in sorted(Path(folder).glob("*.json")):
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            entries.extend(data if isinstance(data, list) else [data])
    return entries


async def ingest():
    conn = await asyncpg.connect(settings.database_url.replace("+asyncpg", ""))

    for book_name, folder in TAFSIR_BOOKS.items():
        if not Path(folder).exists():
            print(f"⚠  Skipping {book_name} — folder not found: {folder}")
            continue

        entries = load_folder(folder)
        batch, total = [], 0

        for entry in entries:
            embedding = embed_passage(entry["content"])
            batch.append((
                entry["surah_number"], entry["ayah_number"],
                book_name, entry["content"], embedding
            ))
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
