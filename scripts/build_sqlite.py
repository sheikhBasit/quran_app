#!/usr/bin/env python3
"""
Build quran.db SQLite database from JSON source files.
Output: composeApp/src/androidMain/assets/quran.db

Usage:
    python scripts/build_sqlite.py

Run audit_jsons.py first and ensure all checks pass.
"""
import sqlite3
import json
from pathlib import Path

DB_OUT = Path("composeApp/src/androidMain/assets/quran.db")
SCHEMA = """
PRAGMA journal_mode=WAL;
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS surahs (
    number                INTEGER PRIMARY KEY,
    name_arabic           TEXT NOT NULL,
    name_english          TEXT NOT NULL,
    name_transliteration  TEXT NOT NULL,
    revelation_type       TEXT NOT NULL,
    ayah_count            INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS quran_pages (
    page_number  INTEGER NOT NULL,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    PRIMARY KEY (surah_number, ayah_number)
);

CREATE TABLE IF NOT EXISTS ayahs (
    id                  INTEGER PRIMARY KEY,
    surah_number        INTEGER NOT NULL,
    ayah_number         INTEGER NOT NULL,
    page_number         INTEGER NOT NULL DEFAULT 1,
    juz_number          INTEGER NOT NULL DEFAULT 1,
    arabic_text_hafs    TEXT NOT NULL,
    arabic_text_warsh   TEXT NOT NULL DEFAULT '',
    translation_english TEXT NOT NULL,
    UNIQUE(surah_number, ayah_number)
);

CREATE TABLE IF NOT EXISTS tafsir (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    book_name    TEXT NOT NULL,
    content      TEXT NOT NULL,
    UNIQUE(surah_number, ayah_number, book_name)
);

CREATE TABLE IF NOT EXISTS hadith (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    collection    TEXT NOT NULL,
    book_number   INTEGER NOT NULL DEFAULT 1,
    hadith_number INTEGER NOT NULL,
    arabic_text   TEXT NOT NULL DEFAULT '',
    translation   TEXT NOT NULL,
    narrator      TEXT NOT NULL DEFAULT '',
    UNIQUE(collection, book_number, hadith_number)
);

CREATE TABLE IF NOT EXISTS bookmarks (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    type         TEXT NOT NULL CHECK(type IN ('ayah','hadith')),
    reference_id INTEGER NOT NULL,
    created_at   TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(type, reference_id)
);

CREATE TABLE IF NOT EXISTS highlights (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    ayah_id    INTEGER NOT NULL UNIQUE,
    color      TEXT NOT NULL DEFAULT '#FFD700',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS notes (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    type         TEXT NOT NULL CHECK(type IN ('ayah','hadith')),
    reference_id INTEGER NOT NULL,
    content      TEXT NOT NULL,
    updated_at   TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(type, reference_id)
);

CREATE TABLE IF NOT EXISTS reading_position (
    id           INTEGER PRIMARY KEY CHECK(id = 1),
    surah_number INTEGER NOT NULL DEFAULT 1,
    ayah_number  INTEGER NOT NULL DEFAULT 1,
    page_number  INTEGER NOT NULL DEFAULT 1,
    mode         TEXT NOT NULL DEFAULT 'scroll'
);

INSERT OR IGNORE INTO reading_position(id) VALUES(1);

CREATE TABLE IF NOT EXISTS app_settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

INSERT OR IGNORE INTO app_settings VALUES('quran_script', 'hafs');
INSERT OR IGNORE INTO app_settings VALUES('theme', 'dark');
INSERT OR IGNORE INTO app_settings VALUES('reading_mode', 'scroll');

CREATE INDEX IF NOT EXISTS idx_ayahs_surah   ON ayahs(surah_number);
CREATE INDEX IF NOT EXISTS idx_ayahs_page    ON ayahs(page_number);
CREATE INDEX IF NOT EXISTS idx_ayahs_search  ON ayahs(translation_english);
CREATE INDEX IF NOT EXISTS idx_tafsir_ref    ON tafsir(surah_number, ayah_number);
CREATE INDEX IF NOT EXISTS idx_hadith_col    ON hadith(collection, book_number);
CREATE INDEX IF NOT EXISTS idx_hadith_search ON hadith(translation);
"""


def load_json_folder(folder: str) -> list:
    entries = []
    for f in sorted(Path(folder).glob("*.json")):
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            entries.extend(data if isinstance(data, list) else [data])
    return entries


def build():
    DB_OUT.parent.mkdir(parents=True, exist_ok=True)
    if DB_OUT.exists():
        DB_OUT.unlink()
        print("🗑  Removed existing quran.db")

    conn = sqlite3.connect(DB_OUT)
    conn.executescript(SCHEMA)
    print("✅ Schema created")

    # Ayahs
    ayahs_path = Path("data/ayahs/quran.json")
    if ayahs_path.exists():
        with open(ayahs_path, encoding="utf-8") as f:
            ayahs = json.load(f)
        conn.executemany(
            "INSERT OR IGNORE INTO ayahs VALUES(?,?,?,?,?,?,?,?)",
            [(a["id"], a["surah_number"], a["ayah_number"],
              a.get("page_number", 1), a.get("juz_number", 1),
              a["arabic_text_hafs"], a.get("arabic_text_warsh", a["arabic_text_hafs"]),
              a["translation_english"]) for a in ayahs]
        )
        print(f"✅ Ayahs: {len(ayahs)}")

    # Tafsir — 3 books
    for book_name, folder in [
        ("ibn_kathir", "data/tafsir/ibn_kathir"),
        ("maarif",     "data/tafsir/maarif"),
        ("ibn_abbas",  "data/tafsir/ibn_abbas"),
    ]:
        if not Path(folder).exists():
            print(f"⚠  Skipping tafsir {book_name} — folder not found")
            continue
        entries = load_json_folder(folder)
        conn.executemany(
            "INSERT OR IGNORE INTO tafsir(surah_number,ayah_number,book_name,content) VALUES(?,?,?,?)",
            [(e["surah_number"], e["ayah_number"], book_name, e["content"]) for e in entries]
        )
        print(f"✅ Tafsir [{book_name}]: {len(entries)}")

    # Hadith — 6 collections
    for collection, folder in [
        ("bukhari",   "data/hadith/bukhari"),
        ("muslim",    "data/hadith/muslim"),
        ("abu_dawud", "data/hadith/abu_dawud"),
        ("tirmidhi",  "data/hadith/tirmidhi"),
        ("nasai",     "data/hadith/nasai"),
        ("ibn_majah", "data/hadith/ibn_majah"),
    ]:
        if not Path(folder).exists():
            print(f"⚠  Skipping hadith {collection} — folder not found")
            continue
        hadiths = load_json_folder(folder)
        conn.executemany(
            "INSERT OR IGNORE INTO hadith(collection,book_number,hadith_number,arabic_text,translation,narrator) VALUES(?,?,?,?,?,?)",
            [(collection, h.get("book_number", 1), h["hadith_number"],
              h.get("arabic_text", ""), h["translation"],
              h.get("narrator", "")) for h in hadiths]
        )
        print(f"✅ Hadith [{collection}]: {len(hadiths)}")

    conn.commit()
    conn.close()

    size_mb = DB_OUT.stat().st_size / 1024 / 1024
    print(f"\n📦 Built: {DB_OUT}  ({size_mb:.1f} MB)")
    if size_mb > 500:
        print("⚠  DB exceeds 500MB — consider compressing Tafsir content")


if __name__ == "__main__":
    build()
