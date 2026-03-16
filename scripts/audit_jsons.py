#!/usr/bin/env python3
"""
Data quality audit script.
Run before building SQLite or ingesting into pgvector.

Usage:
    python scripts/audit_jsons.py
"""
import json
from pathlib import Path


def audit_ayahs(path: str) -> bool:
    p = Path(path)
    if not p.exists():
        print(f"❌ MISSING: {path}")
        return False
    with open(p, encoding="utf-8") as f:
        data = json.load(f)

    issues = []
    for i, a in enumerate(data):
        for k in ["surah_number", "ayah_number", "arabic_text_hafs", "translation_english"]:
            if k not in a:
                issues.append(f"Row {i}: missing '{k}'")
        if not a.get("arabic_text_hafs", "").strip():
            issues.append(f"Row {i}: empty arabic_text_hafs")
        if not a.get("translation_english", "").strip():
            issues.append(f"Row {i}: empty translation_english")

    surahs = {a["surah_number"] for a in data if "surah_number" in a}
    missing_surahs = set(range(1, 115)) - surahs
    if missing_surahs:
        issues.append(f"Missing surahs: {sorted(missing_surahs)[:10]}...")

    status = "✅" if len(issues) == 0 else "❌"
    print(f"{status} Ayahs: {len(data)} records | Issues: {len(issues)}")
    for issue in issues[:10]:
        print(f"   ⚠  {issue}")
    return len(issues) == 0


def audit_tafsir_book(folder: str, book_name: str) -> bool:
    p = Path(folder)
    if not p.exists():
        print(f"❌ MISSING tafsir folder: {folder}")
        return False

    entries = []
    for f in p.glob("*.json"):
        with open(f, encoding="utf-8") as fp:
            entries.extend(json.load(fp))

    if len(entries) == 0:
        print(f"❌ Tafsir [{book_name}]: no entries found")
        return False

    refs = {(e.get("surah_number"), e.get("ayah_number")) for e in entries}
    empty = [e for e in entries if not e.get("content", "").strip()]
    coverage = len(refs)

    status = "✅" if coverage >= 6000 and len(empty) == 0 else "⚠ "
    print(f"{status} Tafsir [{book_name}]: {len(entries)} entries | Coverage: {coverage} ayahs | Empty: {len(empty)}")
    return coverage >= 6000 and len(empty) == 0


def audit_hadith(folder: str, collection: str) -> bool:
    p = Path(folder)
    if not p.exists():
        print(f"❌ MISSING hadith folder: {folder}")
        return False

    entries = []
    for f in p.glob("*.json"):
        with open(f, encoding="utf-8") as fp:
            entries.extend(json.load(fp))

    if len(entries) == 0:
        print(f"❌ Hadith [{collection}]: no entries found")
        return False

    issues = []
    for i, h in enumerate(entries[:200]):
        if "hadith_number" not in h:
            issues.append(f"Row {i}: missing hadith_number")
        if not h.get("translation", "").strip():
            issues.append(f"Row {i}: empty translation")

    status = "✅" if len(issues) == 0 else "❌"
    print(f"{status} Hadith [{collection}]: {len(entries)} records | Issues: {len(issues)}")
    return len(issues) == 0


def main():
    print("=" * 55)
    print("  QURAN APP — DATA QUALITY AUDIT")
    print("=" * 55)

    results = []

    print("\n📖 QURAN AYAHS")
    results.append(audit_ayahs("data/ayahs/quran.json"))

    print("\n📚 TAFSIR BOOKS")
    for book, folder in [
        ("ibn_kathir", "data/tafsir/ibn_kathir"),
        ("maarif",     "data/tafsir/maarif"),
        ("ibn_abbas",  "data/tafsir/ibn_abbas"),
    ]:
        results.append(audit_tafsir_book(folder, book))

    print("\n📜 HADITH COLLECTIONS")
    for collection, folder in [
        ("bukhari",   "data/hadith/bukhari"),
        ("muslim",    "data/hadith/muslim"),
        ("abu_dawud", "data/hadith/abu_dawud"),
        ("tirmidhi",  "data/hadith/tirmidhi"),
        ("nasai",     "data/hadith/nasai"),
        ("ibn_majah", "data/hadith/ibn_majah"),
    ]:
        results.append(audit_hadith(folder, collection))

    print("\n" + "=" * 55)
    passed = sum(1 for r in results if r)
    total = len(results)
    if all(results):
        print(f"✅ ALL {total} CHECKS PASSED — ready to build SQLite")
    else:
        print(f"❌ {total - passed}/{total} CHECKS FAILED — fix data before proceeding")
    print("=" * 55)


if __name__ == "__main__":
    main()
