#!/usr/bin/env python3
"""Ingest Hadith collections into pgvector.
Supports nested Chapter folders and merges Arabic/English.
"""
import json, asyncio
from pathlib import Path
import asyncpg
from app.rag.embedder import embed_passages
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

def load_collection(folder_path: Path) -> list:
    """Recursively load all hadiths from english.json files in chapters."""
    all_hadiths = []
    print(f"DEBUG: Searching in {folder_path.absolute()}")
    count = 0
    for english_file in folder_path.rglob("english.json"):
        count += 1
        try:
            with open(english_file, encoding="utf-8") as f:
                data = json.load(f)
                if "hadiths" in data:
                    all_hadiths.extend(data["hadiths"])
                else:
                    print(f"DEBUG: 'hadiths' key missing in {english_file}")
        except Exception as e:
            print(f"DEBUG: Error loading {english_file}: {e}")
    print(f"DEBUG: Found {count} english.json files. Total hadiths: {len(all_hadiths)}")
    return all_hadiths

async def ingest():
    conn = await asyncpg.connect(settings.database_url.replace("+asyncpg", ""))
    
    for collection, path_str in COLLECTIONS.items():
        folder_path = Path(path_str)
        if not folder_path.exists():
            print(f"⚠ Skipping {collection} - {path_str} not found")
            continue
            
        print(f"📦 Loading {collection}...")
        hadiths = load_collection(folder_path)
        if not hadiths:
            print(f"⚠ No hadiths found for {collection}")
            continue
            
        print(f"🚀 Ingesting {len(hadiths)} hadiths for {collection}...")
        
        # Optimization: Fetch existing hadith numbers to skip them
        existing = await conn.fetch("SELECT hadith_number FROM hadith_embeddings WHERE collection = $1", collection)
        existing_nums = {r["hadith_number"] for r in existing}
        print(f"  (Skipping {len(existing_nums)} already indexed records)")
        
        to_ingest = []
        for h in hadiths:
            h_num = int(h["hadith_number"])
            if h_num not in existing_nums and h.get("text"):
                to_ingest.append(h)

        print(f"🚀 Embedding and Ingesting {len(to_ingest)} new hadiths...")
        
        EMBED_BATCH_SIZE = 64
        total = len(existing_nums)
        
        for i in range(0, len(to_ingest), EMBED_BATCH_SIZE):
            chunk = to_ingest[i : i + EMBED_BATCH_SIZE]
            
            # Prepare contents
            contents = []
            for h in chunk:
                arabic = h.get("arabic_text", "")
                english = h.get("text", "")
                contents.append(f"{arabic} | {english}" if arabic else english)
            
            # Batch Embed
            embeddings = embed_passages(contents)
            
            # Prepare DB batch
            db_batch = []
            for h, content, emb in zip(chunk, contents, embeddings):
                vector_str = "[" + ",".join(map(str, emb)) + "]"
                db_batch.append((collection, int(h["hadith_number"]), content, vector_str))
            
            # Insert
            await conn.executemany(
                "INSERT INTO hadith_embeddings(collection,hadith_number,content,embedding) "
                "VALUES($1,$2,$3,$4::vector) ON CONFLICT DO NOTHING",
                db_batch
            )
            
            total += len(chunk)
            print(f"  [{collection}] {total}/{len(hadiths)}...", end="\r")

        print(f"\n✅ {collection} complete: {total} total entries")

    await conn.close()
    print("\n🎉 All Hadith collections ingested!")

if __name__ == "__main__":
    asyncio.run(ingest())
