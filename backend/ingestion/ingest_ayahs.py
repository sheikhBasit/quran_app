#!/usr/bin/env python3
"""Ingest Quran Ayahs into pgvector.
Run once after docker compose up -d and DB is healthy.
"""
import json, asyncio
from pathlib import Path
import asyncpg
from app.rag.embedder import embed_passage
from app.config import settings

BATCH_SIZE = 100


async def ingest():
    path = Path("data/ayahs/quran.json")
    if not path.exists():
        raise FileNotFoundError("data/ayahs/quran.json not found")

    with open(path, encoding="utf-8") as f:
        ayahs = json.load(f)

    conn = await asyncpg.connect(settings.database_url.replace("+asyncpg", ""))

    batch, total = [], 0
    for ayah in ayahs:
        content = f"{ayah['arabic_text_hafs']} | {ayah['translation_english']}"
        embedding = embed_passage(content)
        batch.append((
            ayah["surah_number"], ayah["ayah_number"],
            content, embedding
        ))
        if len(batch) >= BATCH_SIZE:
            await conn.executemany(
                "INSERT INTO ayah_embeddings(surah_number,ayah_number,content,embedding) "
                "VALUES($1,$2,$3,$4::vector) ON CONFLICT DO NOTHING",
                batch
            )
            total += len(batch)
            print(f"  Ingested {total}/{len(ayahs)} ayahs...", end="\r")
            batch = []

    if batch:
        await conn.executemany(
            "INSERT INTO ayah_embeddings(surah_number,ayah_number,content,embedding) "
            "VALUES($1,$2,$3,$4::vector) ON CONFLICT DO NOTHING",
            batch
        )
        total += len(batch)

    await conn.close()
    print(f"\n✅ Ayahs ingested: {total}")


if __name__ == "__main__":
    asyncio.run(ingest())
