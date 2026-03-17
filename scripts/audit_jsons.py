#!/usr/bin/env python3
"""
Data quality audit script.

Handles:
- Nested chapter subfolders for Hadith (e.g. Chapter_XYZ/hadith.json)
- Separate arabic.json + quran.json + surah_metadata.json structure
- Tafsir as single JSON file (ibn_kathir.json) or folder

Usage: python scripts/audit_jsons.py
"""
import json
from pathlib import Path


def load_recursive(folder: Path) -> list:
    """Load all JSON files recursively, including nested chapter subfolders."""
    entries = []
    for path in sorted(folder.rglob("*.json")):
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
            entries.extend(data if isinstance(data, list) else [data])
        except Exception as e:
            print(f"  ⚠  Cannot parse {path.relative_to(folder)}: {e}")
    return entries


def detect_hadith_fields(entry: dict) -> tuple[str | None, str | None]:
    """Detect which fields hold hadith number and translation text."""
    number_fields = ["hadith_number", "number", "id", "hadith_id", "No"]
    text_fields   = ["translation", "text", "english", "body",
                     "hadith_english", "Translation", "Text"]
    num   = next((f for f in number_fields if f in entry), None)
    trans = next((f for f in text_fields   if f in entry), None)
    return num, trans


# ── Quran ──────────────────────────────────────────────────────────────────────

def audit_quran() -> bool:
    ok = True

    # Support: data/quran.json OR data/ayahs/quran.json
    qpath = Path("data/quran.json")
    if not qpath.exists():
        qpath = Path("data/ayahs/quran.json")
    if not qpath.exists():
        print("❌ MISSING: data/quran.json")
        return False

    with open(qpath, encoding="utf-8") as f:
        raw = json.load(f)

    # Flatten all structures into a simple list of ayah dicts
    ayahs = []
    if isinstance(raw, list):
        for item in raw:
            if isinstance(item, dict) and "ayahs" in item:
                # [{surah_num, surah_name, total_ayahs, ayahs:[...]}]
                s_num = int(item.get("surah_num") or item.get("surah_number") or 0)
                for i, ayah in enumerate(item["ayahs"], 1):
                    if isinstance(ayah, str):
                        ayahs.append({"surah_number": s_num, "ayah_number": i,
                                      "translation_english": ayah})
                    elif isinstance(ayah, dict):
                        # Real fields: ayah_num, translation (may also be text/translation_english)
                        translation = (ayah.get("translation") or ayah.get("text")
                                      or ayah.get("translation_english") or "")
                        a_num = int(ayah.get("ayah_num") or ayah.get("ayah_number") or i)
                        ayahs.append({"surah_number": s_num, "ayah_number": a_num,
                                      "translation_english": translation})
            elif isinstance(item, dict) and "surah_number" in item:
                # flat list [{surah_number, ayah_number, translation_english, ...}]
                ayahs.append(item)
    elif isinstance(raw, dict):
        for s_key, s_val in raw.items():
            if isinstance(s_val, dict):
                for a_key, a_val in s_val.items():
                    text = a_val if isinstance(a_val, str) else a_val.get("translation", "")
                    ayahs.append({"surah_number": int(s_key), "ayah_number": int(a_key),
                                  "translation_english": text})
            elif isinstance(s_val, list):
                for i, a_val in enumerate(s_val, 1):
                    text = a_val if isinstance(a_val, str) else a_val.get("translation", "")
                    ayahs.append({"surah_number": int(s_key), "ayah_number": i,
                                  "translation_english": text})
    else:
        print("❌ quran.json: unrecognised structure"); return False

    surahs  = {a.get("surah_number") for a in ayahs}
    missing = set(range(1, 115)) - surahs
    empty_t = [a for a in ayahs if not str(a.get("translation_english","")).strip()]
    total   = len(ayahs)
    status  = "✅" if total >= 6000 and not missing and not empty_t else "⚠ "
    print(f"{status} quran.json: {total} ayahs | Surahs: {len(surahs)}/114 | Empty: {len(empty_t)}")
    if missing: print(f"   Missing surahs: {sorted(missing)[:10]}")
    if total < 6000: ok = False

    # surah_metadata.json (optional)
    spath = Path("data/surah_metadata.json")
    if spath.exists():
        with open(spath, encoding="utf-8") as f:
            sm = json.load(f)
        print(f"✅ surah_metadata.json: {len(sm) if isinstance(sm,list) else len(sm)} entries")
    else:
        print("⚠  surah_metadata.json not found (optional)")

    return ok


# ── Tafsir ─────────────────────────────────────────────────────────────────────

def audit_tafsir(name: str) -> bool:
    # Support both:  data/tafsir/ibn_kathir.json  and  data/tafsir/ibn_kathir/
    file_path   = Path(f"data/tafsir/{name}.json")
    folder_path = Path(f"data/tafsir/{name}")

    if file_path.exists():
        with open(file_path, encoding="utf-8") as f:
            entries = json.load(f)
        if not isinstance(entries, list): entries = [entries]
    elif folder_path.exists():
        entries = load_recursive(folder_path)
    else:
        print(f"❌ MISSING: data/tafsir/{name}.json  OR  data/tafsir/{name}/")
        return False

    # Real field names: ayat_number (not ayah_number), tafseer (not content)
    refs  = {(e.get("surah_number"), e.get("ayat_number") or e.get("ayah_number")) for e in entries}
    empty = [e for e in entries if not (e.get("tafseer") or e.get("content","")).strip()]
    ok    = len(refs) >= 6000 and not empty
    print(f"{'✅' if ok else '⚠ '} Tafsir [{name}]: {len(entries)} entries | "
          f"Coverage: {len(refs)} ayahs | Empty: {len(empty)}")
    return ok


# ── Hadith ─────────────────────────────────────────────────────────────────────

def audit_hadith(collection: str) -> bool:
    folder = Path(f"data/hadith/{collection}")
    if not folder.exists():
        print(f"❌ MISSING: data/hadith/{collection}/")
        return False

    chapters = [d for d in folder.iterdir() if d.is_dir()]

    # Walk Chapter_XYZ/english.json and extract entries from "hadiths" array
    entries = []
    for chapter_dir in sorted(chapters):
        eng = chapter_dir / "english.json"
        if not eng.exists():
            continue
        with open(eng, encoding="utf-8") as f:
            data = json.load(f)
        entries.extend(data.get("hadiths", []))

    if not entries:
        print(f"❌ Hadith [{collection}]: no entries found"); return False

    # Detect field names from first entry
    sample        = next((e for e in entries if isinstance(e, dict)), {})
    num_f, text_f = detect_hadith_fields(sample)

    issues = []
    for i, h in enumerate(entries[:300]):
        if not isinstance(h, dict): continue
        n, t = detect_hadith_fields(h)
        if not n: issues.append(f"Row {i}: no number field")
        if not t: issues.append(f"Row {i}: no text/translation field")

    ok = len(issues) == 0
    chapter_info = f" | Chapters: {len(chapters)}" if chapters else ""
    print(f"{'✅' if ok else '⚠ '} Hadith [{collection}]: {len(entries)} entries"
          f"{chapter_info} | Issues in sample: {len(issues)}")
    print(f"   Fields detected → number: '{num_f}', translation: '{text_f}'")
    print(f"   All fields: {list(sample.keys())[:8]}")
    if issues[:3]:
        for issue in issues[:3]: print(f"   ⚠  {issue}")
    return ok


# ── Main ───────────────────────────────────────────────────────────────────────

def main():
    print("=" * 62)
    print("  QURAN APP — DATA QUALITY AUDIT")
    print("=" * 62)

    results = []

    print("\n📖 QURAN DATA")
    results.append(audit_quran())

    print("\n📚 TAFSIR BOOKS")
    for name in ["ibn_kathir", "maarif", "ibn_abbas"]:
        results.append(audit_tafsir(name))

    print("\n📜 HADITH COLLECTIONS")
    for col in ["bukhari", "muslim", "abu_dawud", "tirmidhi", "nasai", "ibn_majah"]:
        results.append(audit_hadith(col))

    print("\n" + "=" * 62)
    passed = sum(results)
    total  = len(results)
    if all(results):
        print(f"✅ ALL {total} CHECKS PASSED — ready for build_sqlite.py")
    else:
        print(f"⚠  {passed}/{total} checks passed")
        print("   Check field names shown above — update FIELD_MAP in build_sqlite.py")
    print("=" * 62)


if __name__ == "__main__":
    main()
