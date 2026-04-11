#!/usr/bin/env python3
"""
Seed script: fetch Quran word-by-word data from quran.com API and insert into SQLite word_meanings table.
Run AFTER Task 2 (WordMeanings.sq schema is applied): python3 data/seed_words.py
Requires: pip install requests
"""
import sqlite3
import requests
import sys
import os
import time

# Find DB — try common locations
_CANDIDATES = [
    os.path.join(os.path.dirname(__file__), "../composeApp/src/androidMain/assets/quran.db"),
    os.path.join(os.path.dirname(__file__), "../composeApp/src/commonMain/resources/quran.db"),
]
DB_PATH = next((p for p in _CANDIDATES if os.path.exists(os.path.abspath(p))), None)


def fetch_verses(surah_number: int) -> list[dict]:
    url = f"https://api.qurancdn.com/api/qdc/verses/by_chapter/{surah_number}"
    params = {
        "words": "true",
        "word_fields": "text_uthmani,transliteration,translation",
        "per_page": 300,
        "page": 1,
    }
    for attempt in range(3):
        try:
            resp = requests.get(url, params=params, timeout=15)
            resp.raise_for_status()
            return resp.json().get("verses", [])
        except Exception as e:
            if attempt == 2:
                raise
            print(f"  Retry {attempt+1} for surah {surah_number}: {e}")
            time.sleep(2)
    return []  # unreachable — final attempt always raises; satisfies type checker


def seed_surah(conn: sqlite3.Connection, surah_number: int) -> int:
    verses = fetch_verses(surah_number)
    count = 0
    cursor = conn.cursor()
    for verse in verses:
        ayah_number = verse["verse_number"]
        for word in verse.get("words", []):
            if word.get("char_type_name") == "end":
                continue  # skip ayah-end marker (ۚ etc.)
            pos = word.get("position", 0)
            arabic = word.get("text_uthmani", "")
            transliteration = (word.get("transliteration") or {}).get("text", "")
            english = (word.get("translation") or {}).get("text", "")
            cursor.execute(
                """
                INSERT OR IGNORE INTO word_meanings
                    (surah_number, ayah_number, word_position,
                     arabic_word, transliteration, english_meaning)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (surah_number, ayah_number, pos, arabic, transliteration, english),
            )
            if cursor.rowcount > 0:
                count += 1
    conn.commit()
    return count


def main():
    global DB_PATH
    if DB_PATH is None:
        # Try to find it dynamically
        import glob
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        matches = glob.glob(os.path.join(project_root, "**/*.db"), recursive=True)
        if matches:
            DB_PATH = matches[0]
            print(f"Found DB at: {DB_PATH}")
        else:
            print("ERROR: quran.db not found. Run this after Task 2 schema migration.")
            sys.exit(1)

    DB_PATH = os.path.abspath(DB_PATH)
    if not os.path.exists(DB_PATH):
        print(f"ERROR: DB not found at {DB_PATH}")
        sys.exit(1)

    print(f"Using DB: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)

    # Verify table exists
    tables = conn.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='word_meanings'").fetchall()
    if not tables:
        print("ERROR: word_meanings table not found. Run Task 2 (SQLDelight schema) first.")
        conn.close()
        sys.exit(1)

    total = 0
    for surah in range(1, 115):
        n = seed_surah(conn, surah)
        total += n
        print(f"Surah {surah:3d}: {n:4d} words  (running total: {total})")

    conn.close()
    print(f"\nDone. {total} word records seeded into word_meanings.")


if __name__ == "__main__":
    main()
