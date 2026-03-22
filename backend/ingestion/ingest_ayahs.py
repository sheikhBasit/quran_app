#!/usr/bin/env python3
"""Ingest Quran Ayahs into pgvector.
Merges Arabic text from arabic.json and English from quran.json.
"""
import json, asyncio
from pathlib import Path
import asyncpg
from app.rag.embedder import embed_passages
from app.config import settings

BATCH_SIZE = 100

async def ingest():
    arabic_path = Path("data/ayahs/arabic.json")
    english_path = Path("data/ayahs/quran.json")
    
    if not arabic_path.exists() or not english_path.exists():
        raise FileNotFoundError("Ayah data files not found in data/ayahs/")

    with open(arabic_path, encoding="utf-8") as f:
        arabic_data = json.load(f)
    with open(english_path, encoding="utf-8") as f:
        english_data = json.load(f)

    # Flatten English data for easy lookup
    english_lookup = {}
    for surah in english_data:
        s_num = surah["surah_num"]
        for ayah in surah["ayahs"]:
            a_num = ayah["ayah_num"]
            english_lookup[(s_num, a_num)] = ayah["translation"]

    conn = await asyncpg.connect(settings.database_url.replace("+asyncpg", ""))
    total = 0

    print(f"🚀 Embedding and Ingesting {len(arabic_data)} ayahs...")
    EMBED_BATCH_SIZE = 64
    for i in range(0, len(arabic_data), EMBED_BATCH_SIZE):
        chunk = arabic_data[i : i + EMBED_BATCH_SIZE]
        
        contents = []
        for a in chunk:
            s_num = int(a["surah_number"])
            a_num = int(a["ayat_number"])
            english_text = english_lookup.get((s_num, a_num), "")
            contents.append(f"{a['arabic_text']} | {english_text}")
        
        embeddings = embed_passages(contents)
        
        db_batch = []
        for a, content, emb in zip(chunk, contents, embeddings):
            vector_str = "[" + ",".join(map(str, emb)) + "]"
            db_batch.append((int(a["surah_number"]), int(a["ayat_number"]), content, vector_str))
            
        await conn.executemany(
            "INSERT INTO ayah_embeddings(surah_number,ayah_number,content,embedding) "
            "VALUES($1,$2,$3,$4::vector) ON CONFLICT DO NOTHING",
            db_batch
        )
        total += len(chunk)
        print(f"  Ingested {total}/{len(arabic_data)} ayahs...", end="\r")

    await conn.close()
    print(f"\n✅ Ayahs ingested: {total}")

if __name__ == "__main__":
    asyncio.run(ingest())
