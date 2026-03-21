#!/usr/bin/env python3
"""
Build quran.db SQLite database from JSON source files.

Real Hadith structure:
  data/hadith/{collection}/
    Chapter_XYZ/
      english.json  <- {"collection":..., "chapter_name":..., "hadiths":[{hadith_number, text, arabic_text}]}
      urdu.json

Output: composeApp/src/androidMain/assets/quran.db
Usage:  python scripts/build_sqlite.py
"""
import sqlite3, json
from pathlib import Path

DB_OUT = Path("composeApp/src/androidMain/assets/quran.db")

SCHEMA = """
PRAGMA journal_mode=WAL;
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS surahs (
    number INTEGER PRIMARY KEY, name_arabic TEXT NOT NULL,
    name_english TEXT NOT NULL, name_transliteration TEXT NOT NULL,
    revelation_type TEXT NOT NULL, ayah_count INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS ayahs (
    id INTEGER PRIMARY KEY, surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL, page_number INTEGER NOT NULL DEFAULT 1,
    juz_number INTEGER NOT NULL DEFAULT 1, arabic_text_hafs TEXT NOT NULL,
    arabic_text_warsh TEXT NOT NULL DEFAULT '', translation_english TEXT NOT NULL,
    UNIQUE(surah_number, ayah_number)
);
CREATE TABLE IF NOT EXISTS tafsir (
    id INTEGER PRIMARY KEY AUTOINCREMENT, surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL, book_name TEXT NOT NULL, content TEXT NOT NULL,
    UNIQUE(surah_number, ayah_number, book_name)
);
CREATE TABLE IF NOT EXISTS hadith (
    id INTEGER PRIMARY KEY AUTOINCREMENT, collection TEXT NOT NULL,
    chapter_name TEXT NOT NULL DEFAULT '', hadith_number INTEGER NOT NULL,
    arabic_text TEXT NOT NULL DEFAULT '', translation TEXT NOT NULL,
    narrator TEXT NOT NULL DEFAULT '',
    UNIQUE(collection, chapter_name, hadith_number)
);
CREATE TABLE IF NOT EXISTS bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL CHECK(type IN ('ayah','hadith')),
    reference_id INTEGER NOT NULL, created_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(type, reference_id)
);
CREATE TABLE IF NOT EXISTS highlights (
    id INTEGER PRIMARY KEY AUTOINCREMENT, ayah_id INTEGER NOT NULL UNIQUE,
    color TEXT NOT NULL DEFAULT '#FFD700', created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL CHECK(type IN ('ayah','hadith')),
    reference_id INTEGER NOT NULL, content TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')), UNIQUE(type, reference_id)
);
CREATE TABLE IF NOT EXISTS reading_position (
    id INTEGER PRIMARY KEY CHECK(id=1), surah_number INTEGER NOT NULL DEFAULT 1,
    ayah_number INTEGER NOT NULL DEFAULT 1, page_number INTEGER NOT NULL DEFAULT 1,
    mode TEXT NOT NULL DEFAULT 'scroll'
);
INSERT OR IGNORE INTO reading_position(id) VALUES(1);
CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT NOT NULL);
INSERT OR IGNORE INTO app_settings VALUES('quran_script','hafs');
INSERT OR IGNORE INTO app_settings VALUES('theme','dark');
INSERT OR IGNORE INTO app_settings VALUES('reading_mode','scroll');
CREATE INDEX IF NOT EXISTS idx_ayahs_surah   ON ayahs(surah_number);
CREATE INDEX IF NOT EXISTS idx_ayahs_page    ON ayahs(page_number);
CREATE INDEX IF NOT EXISTS idx_ayahs_search  ON ayahs(translation_english);
CREATE INDEX IF NOT EXISTS idx_tafsir_ref    ON tafsir(surah_number, ayah_number);
CREATE INDEX IF NOT EXISTS idx_hadith_col    ON hadith(collection);
CREATE INDEX IF NOT EXISTS idx_hadith_search ON hadith(translation);
"""

def load_json_folder(folder: str) -> list:
    entries = []
    for f in sorted(Path(folder).glob("*.json")):
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            entries.extend(data if isinstance(data, list) else [data])
    return entries

def load_hadith_collection(folder: Path, collection_key: str) -> list:
    """
    Navigate: folder/Chapter_XYZ/english.json
    Each english.json: {"chapter_name":..., "hadiths":[{hadith_number, text, arabic_text}]}
    Returns list of (collection, chapter_name, hadith_number, arabic_text, translation)
    """
    rows = []
    chapter_dirs = sorted([d for d in folder.iterdir() if d.is_dir()])
    for chapter_dir in chapter_dirs:
        eng = chapter_dir / "english.json"
        if not eng.exists():
            continue
        with open(eng, encoding="utf-8") as f:
            data = json.load(f)
        chapter_name = data.get("chapter_name", chapter_dir.name)
        for h in data.get("hadiths", []):
            num         = h.get("hadith_number")
            translation = h.get("text", "").strip()
            arabic      = h.get("arabic_text", "").strip()
            
            # Extract narrator from translation text
            narrator = ""
            if "Narrated" in translation:
                parts = translation.split(":", 1)
                if len(parts) > 1 and "Narrated" in parts[0]:
                    narrator = parts[0].replace("Narrated ", "").strip()
                    translation = parts[1].strip()

            if not num or not translation:
                continue
            rows.append((collection_key, chapter_name, int(num), arabic, translation, narrator))
    return rows

def build():
    DB_OUT.parent.mkdir(parents=True, exist_ok=True)
    if DB_OUT.exists():
        DB_OUT.unlink()
        print("🗑  Removed existing quran.db")

    conn = sqlite3.connect(DB_OUT)
    conn.executescript(SCHEMA)
    print("✅ Schema created\n")

    # 1. Surahs — from surah_metadata.json
    m_path = Path("data/ayahs/surah_metadata.json")
    if m_path.exists():
        with open(m_path, encoding="utf-8") as f:
            metadata = json.load(f)
        s_rows = []
        for s in metadata:
            s_rows.append((
                s["surah_number"],
                s["name_urdu"], # Use Urdu name as Arabic name
                s["name_english"].title(),
                s["name_english"].title(),
                "Meccan", # Default if not in json
                s["total_ayat"]
            ))
        conn.executemany("INSERT OR IGNORE INTO surahs VALUES(?,?,?,?,?,?)", s_rows)
        print(f"✅ Surahs: {len(s_rows)} rows inserted")

    # 2. Ayahs — Merging arabic.json and quran.json
    a_path = Path("data/ayahs/arabic.json")
    q_path = Path("data/quran.json")
    
    if a_path.exists() and q_path.exists():
        with open(a_path, encoding="utf-8") as f:
            arabic_data = json.load(f)
        with open(q_path, encoding="utf-8") as f:
            quran_data = json.load(f)

        # Create translation map: (surah, ayah) -> translation
        trans_map = {}
        for surah in quran_data:
            s_num = surah["surah_num"]
            for ayah in surah["ayahs"]:
                a_num = ayah["ayah_num"]
                trans_map[(s_num, a_num)] = ayah["translation"]

        rows = []
        for i, a in enumerate(arabic_data, 1):
            s_num = a["surah_number"]
            a_num = int(a["ayat_number"])
            arabic = a["arabic_text"]
            translation = trans_map.get((s_num, a_num), "")
            rows.append((i, s_num, a_num, 1, 1, arabic, arabic, translation))

        conn.executemany("INSERT OR IGNORE INTO ayahs VALUES(?,?,?,?,?,?,?,?)", rows)
        print(f"✅ Ayahs: {len(rows)} rows inserted from arabic.json")
    else:
        print("⚠  arabic.json or quran.json missing — falling back to old logic")
        # [Old fallback logic remains or simplified]
        p = q_path if q_path.exists() else Path("data/ayahs/quran.json")
        if p.exists():
            with open(p, encoding="utf-8") as f:
                raw = json.load(f)
            rows = []; row_id = 1
            if isinstance(raw, list) and raw and "ayahs" in raw[0]:
                for surah in raw:
                    s_num = int(surah.get("surah_num") or 0)
                    for i, ayah in enumerate(surah.get("ayahs", []), 1):
                        translation = ayah.get("translation", "")
                        arabic = ayah.get("arabic", "")
                        a_num = int(ayah.get("ayah_num") or i)
                        rows.append((row_id, s_num, a_num, 1, 1, arabic, arabic, translation))
                        row_id += 1
            conn.executemany("INSERT OR IGNORE INTO ayahs VALUES(?,?,?,?,?,?,?,?)", rows)

    # Tafsir — 3 books (single .json files; fields: ayat_number, tafseer)
    for book_name in ["ibn_kathir", "maarif", "ibn_abbas"]:
        json_file = Path(f"data/tafsir/{book_name}.json")
        folder    = Path(f"data/tafsir/{book_name}")
        if json_file.exists():
            with open(json_file, encoding="utf-8") as f:
                entries = json.load(f)
        elif folder.exists():
            entries = load_json_folder(str(folder))
        else:
            print(f"⚠  Skipping tafsir {book_name} — not found"); continue
        rows = []
        for e in entries:
            surah   = e.get("surah_number")
            ayah    = e.get("ayat_number") or e.get("ayah_number")
            content = (e.get("tafseer") or e.get("content", "")).strip()
            if surah and ayah and content:
                rows.append((surah, ayah, book_name, content))
        conn.executemany(
            "INSERT OR IGNORE INTO tafsir(surah_number,ayah_number,book_name,content) VALUES(?,?,?,?)",
            rows)
        print(f"✅ Tafsir [{book_name}]: {len(rows)} entries")

    # Hadith — 6 collections (nested Chapter_XYZ/english.json)
    for col_key, folder_str in [("bukhari","data/hadith/bukhari"),
                                 ("muslim","data/hadith/muslim"),
                                 ("abu_dawud","data/hadith/abu_dawud"),
                                 ("tirmidhi","data/hadith/tirmidhi"),
                                 ("nasai","data/hadith/nasai"),
                                 ("ibn_majah","data/hadith/ibn_majah")]:
        folder = Path(folder_str)
        if not folder.exists():
            print(f"⚠  Skipping hadith {col_key}"); continue
        rows = load_hadith_collection(folder, col_key)
        if not rows:
            print(f"⚠  Hadith [{col_key}]: no rows extracted"); continue
        conn.executemany(
            "INSERT OR IGNORE INTO hadith(collection,chapter_name,hadith_number,arabic_text,translation,narrator) VALUES(?,?,?,?,?,?)",
            rows)
        print(f"✅ Hadith [{col_key}]: {len(rows)}")

    conn.commit(); conn.close()
    size_mb = DB_OUT.stat().st_size / 1024 / 1024
    print(f"\n📦 Built: {DB_OUT}  ({size_mb:.1f} MB)")
    if size_mb > 500:
        print("⚠  DB > 500 MB — consider compressing Tafsir")

if __name__ == "__main__":
    build()
