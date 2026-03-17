"""
TDD tests for raw JSON data quality.
Run BEFORE building SQLite or running ingestion.

Hadith real structure:
  data/hadith/{collection}/
    Chapter_XYZ/
      english.json  <- {"chapter_name":..., "total_hadiths":...,
                        "hadiths":[{hadith_number, text, arabic_text, reference, baab}]}
      urdu.json
"""
import json
import pytest
from pathlib import Path


# ── Ayahs ─────────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def ayahs():
    p = Path("data/ayahs/quran.json")
    if not p.exists():
        pytest.skip("data/ayahs/quran.json not found")
    with open(p, encoding="utf-8") as f:
        return json.load(f)


class TestAyahs:
    def test_total_count(self, ayahs):
        assert len(ayahs) == 6236, f"Expected 6236, got {len(ayahs)}"

    def test_all_114_surahs_present(self, ayahs):
        assert {a["surah_number"] for a in ayahs} == set(range(1, 115))

    def test_no_empty_arabic_hafs(self, ayahs):
        empty = [a for a in ayahs if not a.get("arabic_text_hafs","").strip()]
        assert len(empty) == 0

    def test_no_empty_translation(self, ayahs):
        empty = [a for a in ayahs if not a.get("translation_english","").strip()]
        assert len(empty) == 0

    def test_al_fatiha_has_7_ayahs(self, ayahs):
        assert len([a for a in ayahs if a["surah_number"] == 1]) == 7

    def test_al_baqarah_has_286_ayahs(self, ayahs):
        assert len([a for a in ayahs if a["surah_number"] == 2]) == 286


# ── Tafsir ────────────────────────────────────────────────────────────────────

@pytest.mark.parametrize("book,folder", [
    ("ibn_kathir", "data/tafsir/ibn_kathir"),
    ("maarif",     "data/tafsir/maarif"),
    ("ibn_abbas",  "data/tafsir/ibn_abbas"),
])
class TestTafsir:
    """
    Real data: data/tafsir/{book}.json  (single flat file)
    Fields: surah_number, ayat_number, tafseer
    """
    def _load(self, book, folder):
        json_file = Path(f"data/tafsir/{book}.json")
        p = Path(folder)
        if json_file.exists():
            with open(json_file, encoding="utf-8") as fp:
                entries = json.load(fp)
            return entries if isinstance(entries, list) else [entries]
        elif p.exists():
            entries = []
            for f in p.glob("*.json"):
                with open(f, encoding="utf-8") as fp:
                    entries.extend(json.load(fp))
            return entries
        else:
            pytest.skip(f"Neither data/tafsir/{book}.json nor {folder}/ found")

    def _content(self, e):
        return (e.get("tafseer") or e.get("content") or "").strip()

    def _ayah_num(self, e):
        return e.get("ayat_number") or e.get("ayah_number")

    def test_folder_exists(self, book, folder):
        json_file = Path(f"data/tafsir/{book}.json")
        assert json_file.exists() or Path(folder).exists(),             f"Missing: data/tafsir/{book}.json"

    def test_has_entries(self, book, folder):
        assert len(self._load(book, folder)) > 0

    def test_covers_minimum_ayahs(self, book, folder):
        entries = self._load(book, folder)
        refs = {(e["surah_number"], self._ayah_num(e)) for e in entries}
        assert len(refs) >= 6000, f"{book} covers only {len(refs)} refs"

    def test_no_empty_content(self, book, folder):
        entries = self._load(book, folder)
        empty = [e for e in entries if not self._content(e)]
        assert len(empty) == 0, f"{book}: {len(empty)} empty entries"

    def test_has_required_fields(self, book, folder):
        for e in self._load(book, folder)[:100]:
            assert "surah_number" in e
            assert self._ayah_num(e) is not None, f"missing ayat_number/ayah_number"
            assert self._content(e), f"empty tafseer/content"


# ── Hadith — nested Chapter_XYZ/english.json structure ───────────────────────

@pytest.mark.parametrize("collection,folder", [
    ("bukhari",   "data/hadith/bukhari"),
    ("muslim",    "data/hadith/muslim"),
    ("abu_dawud", "data/hadith/abu_dawud"),
    ("tirmidhi",  "data/hadith/tirmidhi"),
    ("nasai",     "data/hadith/nasai"),
    ("ibn_majah", "data/hadith/ibn_majah"),
])
class TestHadith:

    def _load_all(self, folder: str) -> list:
        """Walk Chapter_XYZ/english.json and flatten all hadith entries."""
        p = Path(folder)
        if not p.exists():
            pytest.skip(f"{folder} not found")
        all_hadiths = []
        for chapter_dir in sorted(p.iterdir()):
            if not chapter_dir.is_dir():
                continue
            eng = chapter_dir / "english.json"
            if not eng.exists():
                continue
            with open(eng, encoding="utf-8") as f:
                data = json.load(f)
            all_hadiths.extend(data.get("hadiths", []))
        return all_hadiths

    def _load_chapter_files(self, folder: str) -> list[dict]:
        """Load top-level chapter JSON objects (not the hadith entries within)."""
        p = Path(folder)
        if not p.exists():
            pytest.skip(f"{folder} not found")
        chapters = []
        for chapter_dir in sorted(p.iterdir()):
            if not chapter_dir.is_dir():
                continue
            eng = chapter_dir / "english.json"
            if not eng.exists():
                continue
            with open(eng, encoding="utf-8") as f:
                chapters.append(json.load(f))
        return chapters

    # ── Folder / structure tests ──────────────────────────────────────────────

    def test_folder_exists(self, collection, folder):
        assert Path(folder).exists(), f"Missing folder: {folder}"

    def test_has_chapter_subfolders(self, collection, folder):
        p = Path(folder)
        dirs = [d for d in p.iterdir() if d.is_dir()]
        assert len(dirs) > 0, f"{collection}: no chapter subdirectories found"

    def test_each_chapter_has_english_json(self, collection, folder):
        p = Path(folder)
        missing = []
        for d in p.iterdir():
            if d.is_dir() and not (d / "english.json").exists():
                missing.append(d.name)
        assert len(missing) == 0, \
            f"{collection}: {len(missing)} chapters missing english.json: {missing[:5]}"

    # ── Chapter-level structure ───────────────────────────────────────────────

    def test_chapter_has_hadiths_array(self, collection, folder):
        chapters = self._load_chapter_files(folder)
        assert len(chapters) > 0
        for ch in chapters[:10]:
            assert "hadiths" in ch, \
                f"{collection}: chapter missing 'hadiths' key. Keys found: {list(ch.keys())}"

    def test_chapter_has_chapter_name(self, collection, folder):
        chapters = self._load_chapter_files(folder)
        for ch in chapters[:10]:
            assert "chapter_name" in ch, \
                f"{collection}: chapter missing 'chapter_name'. Keys: {list(ch.keys())}"

    # ── Hadith entry fields ───────────────────────────────────────────────────

    def test_has_hadith_entries(self, collection, folder):
        hadiths = self._load_all(folder)
        assert len(hadiths) > 0, f"{collection}: zero hadith entries found"

    def test_hadith_number_field_present(self, collection, folder):
        hadiths = self._load_all(folder)
        missing = [i for i, h in enumerate(hadiths[:200])
                   if "hadith_number" not in h]
        assert len(missing) == 0, \
            f"{collection}: {len(missing)} entries missing 'hadith_number'"

    def test_text_field_not_empty(self, collection, folder):
        hadiths = self._load_all(folder)
        empty = [i for i, h in enumerate(hadiths[:200])
                 if not h.get("text","").strip()]
        assert len(empty) == 0, \
            f"{collection}: {len(empty)} entries have empty 'text' field"

    def test_arabic_text_field_present(self, collection, folder):
        hadiths = self._load_all(folder)
        # Arabic text should exist on most entries (some collections may have gaps)
        has_arabic = sum(1 for h in hadiths if h.get("arabic_text","").strip())
        assert has_arabic > 0, \
            f"{collection}: no entries have 'arabic_text' field"

    def test_hadith_numbers_are_integers(self, collection, folder):
        hadiths = self._load_all(folder)
        for h in hadiths[:100]:
            num = h.get("hadith_number")
            assert num is not None
            assert isinstance(num, (int, float)), \
                f"{collection}: hadith_number '{num}' is not a number"

    def test_minimum_hadith_count(self, collection, folder):
        hadiths = self._load_all(folder)
        # Even smallest collection should have hundreds
        assert len(hadiths) >= 100, \
            f"{collection}: only {len(hadiths)} hadiths found — expected at least 100"

    def test_sample_entry_structure(self, collection, folder):
        """Print first entry to help debug field names."""
        hadiths = self._load_all(folder)
        assert len(hadiths) > 0
        first = hadiths[0]
        # Verify the fields we depend on
        assert "hadith_number" in first, \
            f"{collection} first entry keys: {list(first.keys())}"
        assert "text" in first, \
            f"{collection} first entry keys: {list(first.keys())}"
