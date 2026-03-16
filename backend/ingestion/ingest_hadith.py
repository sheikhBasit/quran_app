#!/usr/bin/env python3
"""Ingest 6 Hadith collections into pgvector."""
import json, asyncio
from pathlib import Path
import asyncpg
from app.rag.embedder import embed_passage
from app.config import settings

COLLECTIONS = {
    "bukhari":   "data/hadith/bukhari",
    "muslim":    "data/hadith/muslim",
    "abu_dawud": "data/hadith/abu_dawud",
    "tirmidhi":  "data/hadith/tirmidhi",
    "nasai":     "data/hadith/nasai",
    "ibn_majah": "data/hadith/ibn_majah",
}
BATCH_SIZE = 100


def load_folder(folder: str) -> list:
    entries = []
    for f in sorted(Path(folder).glob("*.json")):
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            entries.extend(data if isinstance(data, list) else [data])
    return entries


async def ingest():
    conn = await asyncpg.connect(settings.database_url.replace("+asyncpg", ""))

    for collection, folder in COLLECTIONS.items():
        if not Path(folder).exists():
            print(f"⚠  Skipping {collection} — folder not found: {folder}")
            continue

        hadiths = load_folder(folder)
        batch, total = [], 0

        for h in hadiths:
            narrator = h.get("narrator", "")
            content = f"{h['translation']}" + (f" — Narrator: {narrator}" if narrator else "")
            embedding = embed_passage(content)
            batch.append((collection, h["hadith_number"], content, embedding))

            if len(batch) >= BATCH_SIZE:
                await conn.executemany(
                    "INSERT INTO hadith_embeddings(collection,hadith_number,content,embedding) "
                    "VALUES($1,$2,$3,$4::vector) ON CONFLICT DO NOTHING",
                    batch
                )
                total += len(batch)
                print(f"  [{collection}] {total}/{len(hadiths)}...", end="\r")
                batch = []

        if batch:
            await conn.executemany(
                "INSERT INTO hadith_embeddings(collection,hadith_number,content,embedding) "
                "VALUES($1,$2,$3,$4::vector) ON CONFLICT DO NOTHING",
                batch
            )
            total += len(batch)

        print(f"\n✅ Hadith [{collection}]: {total} entries ingested")

    await conn.close()


if __name__ == "__main__":
    asyncio.run(ingest())
