"""TDD tests for raw JSON data quality.
Run these BEFORE building SQLite or running ingestion.
All must pass before Phase 2 begins.
"""
import json
import pytest
from pathlib import Path


# ─── Ayahs ────────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def ayahs():
    path = Path("data/ayahs/quran.json")
    if not path.exists():
        pytest.skip("data/ayahs/quran.json not found")
    with open(path, encoding="utf-8") as f:
        return json.load(f)


class TestAyahs:
    def test_total_count(self, ayahs):
        assert len(ayahs) == 6236, f"Expected 6236, got {len(ayahs)}"

    def test_all_114_surahs_present(self, ayahs):
        surahs = {a["surah_number"] for a in ayahs}
        assert surahs == set(range(1, 115))

    def test_no_empty_arabic_hafs(self, ayahs):
        empty = [a for a in ayahs if not a.get("arabic_text_hafs", "").strip()]
        assert len(empty) == 0, f"{len(empty)} ayahs have empty Hafs text"

    def test_no_empty_translation(self, ayahs):
        empty = [a for a in ayahs if not a.get("translation_english", "").strip()]
        assert len(empty) == 0, f"{len(empty)} ayahs have empty translation"

    def test_al_fatiha_has_7_ayahs(self, ayahs):
        fatiha = [a for a in ayahs if a["surah_number"] == 1]
        assert len(fatiha) == 7

    def test_al_baqarah_has_286_ayahs(self, ayahs):
        baqarah = [a for a in ayahs if a["surah_number"] == 2]
        assert len(baqarah) == 286


# ─── Tafsir ───────────────────────────────────────────────────────────────────

@pytest.mark.parametrize("book,folder", [
    ("ibn_kathir", "data/tafsir/ibn_kathir"),
    ("maarif",     "data/tafsir/maarif"),
    ("ibn_abbas",  "data/tafsir/ibn_abbas"),
])
class TestTafsir:
    def _load(self, folder):
        path = Path(folder)
        if not path.exists():
            pytest.skip(f"{folder} not found")
        entries = []
        for f in path.glob("*.json"):
            with open(f, encoding="utf-8") as fp:
                entries.extend(json.load(fp))
        return entries

    def test_folder_exists(self, book, folder):
        assert Path(folder).exists(), f"Missing folder: {folder}"

    def test_has_entries(self, book, folder):
        entries = self._load(folder)
        assert len(entries) > 0, f"{book} has no entries"

    def test_covers_minimum_ayahs(self, book, folder):
        entries = self._load(folder)
        refs = {(e["surah_number"], e["ayah_number"]) for e in entries}
        assert len(refs) >= 6000, f"{book} covers only {len(refs)} ayahs"

    def test_no_empty_content(self, book, folder):
        entries = self._load(folder)
        empty = [e for e in entries if not e.get("content", "").strip()]
        assert len(empty) == 0, f"{book} has {len(empty)} empty entries"

    def test_has_required_fields(self, book, folder):
        entries = self._load(folder)
        for e in entries[:100]:
            assert "surah_number" in e, f"{book}: missing surah_number"
            assert "ayah_number" in e, f"{book}: missing ayah_number"
            assert "content" in e, f"{book}: missing content"


# ─── Hadith ───────────────────────────────────────────────────────────────────

@pytest.mark.parametrize("collection,folder", [
    ("bukhari",   "data/hadith/bukhari"),
    ("muslim",    "data/hadith/muslim"),
    ("abu_dawud", "data/hadith/abu_dawud"),
    ("tirmidhi",  "data/hadith/tirmidhi"),
    ("nasai",     "data/hadith/nasai"),
    ("ibn_majah", "data/hadith/ibn_majah"),
])
class TestHadith:
    def _load(self, folder):
        path = Path(folder)
        if not path.exists():
            pytest.skip(f"{folder} not found")
        entries = []
        for f in path.glob("*.json"):
            with open(f, encoding="utf-8") as fp:
                entries.extend(json.load(fp))
        return entries

    def test_folder_exists(self, collection, folder):
        assert Path(folder).exists(), f"Missing folder: {folder}"

    def test_has_entries(self, collection, folder):
        entries = self._load(folder)
        assert len(entries) > 0, f"{collection} has no entries"

    def test_required_fields_present(self, collection, folder):
        entries = self._load(folder)
        for h in entries[:100]:
            assert "hadith_number" in h, f"[{collection}] missing hadith_number"
            assert h.get("translation", "").strip(), f"[{collection}] empty translation"

    def test_no_empty_translations(self, collection, folder):
        entries = self._load(folder)
        empty = [h for h in entries if not h.get("translation", "").strip()]
        assert len(empty) == 0, f"{collection} has {len(empty)} empty translations"
