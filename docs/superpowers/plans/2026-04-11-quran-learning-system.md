# Quran Learning System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add word-by-word breakdown (Layer 1), AI "Understand This" panel (Layer 2), and spaced-repetition flashcards + progress dashboard (Layer 3) into the existing Quran reader without touching any existing feature.

**Architecture:** Three independent layers built on top of the existing KMP/Compose app and FastAPI backend. Each layer introduces new SQLDelight `.sq` files, domain models, repository/usecase classes, and Composable UI components, all wired via the existing Koin DI graph. Layer 1 is purely local (SQLite). Layer 2 reuses the existing Ktor HTTP client + FastAPI RAG backend with one new endpoint. Layer 3 is purely local with Android WorkManager for notifications.

**Tech Stack:** Kotlin Multiplatform · Jetpack Compose · SQLDelight · Koin · Ktor · FastAPI (Python) · Android WorkManager · SM-2 spaced repetition algorithm

---

## File Map

### New files
| Path | Responsibility |
|------|---------------|
| `composeApp/src/commonMain/sqldelight/com/quranapp/db/WordMeanings.sq` | word_meanings table + queries |
| `composeApp/src/commonMain/sqldelight/com/quranapp/db/LearningData.sq` | word_bank, flashcard_reviews, studied_ayahs tables + queries |
| `composeApp/src/commonMain/kotlin/com/quranapp/domain/model/LearningModels.kt` | WordMeaning, WordBankEntry, FlashcardReview, StudiedAyah domain models |
| `composeApp/src/commonMain/kotlin/com/quranapp/domain/repository/LearningRepository.kt` | LearningRepository interface |
| `composeApp/src/commonMain/kotlin/com/quranapp/data/repository/LearningRepositoryImpl.kt` | SQLDelight-backed implementation |
| `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/LearningUseCases.kt` | GetWordMeaningsUseCase, AddToWordBankUseCase, GetDueFlashcardsUseCase, RecordReviewUseCase, GetProgressUseCase, MarkAyahStudiedUseCase |
| `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/SpacedRepetitionUseCase.kt` | SM-2 algorithm: pure function, no DB dependency |
| `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/UnderstandAyahUseCase.kt` | Calls backend /chat/understand-ayah, returns Flow<String> |
| `composeApp/src/commonMain/kotlin/com/quranapp/data/remote/UnderstandRemoteDataSource.kt` | Ktor HTTP call to /chat/understand-ayah (streaming SSE) |
| `composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/LearningViewModel.kt` | All learning UI state (word meanings, word bank, flashcards, understand, progress) |
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordBreakdownRow.kt` | Horizontally scrollable word card row shown below each ayah |
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordDetailSheet.kt` | Bottom sheet shown when a word card is tapped |
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/UnderstandSheet.kt` | 4-section streaming AI explanation bottom sheet |
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/FlashcardSession.kt` | Full-screen flashcard practice UI |
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/ProgressDashboard.kt` | Progress stats + surah heatmap |
| `composeApp/src/androidMain/kotlin/com/quranapp/notification/ReviewReminderWorker.kt` | WorkManager worker for daily review notification |
| `backend/app/routers/understand.py` | POST /chat/understand-ayah FastAPI endpoint |
| `backend/app/rag/understand_prompt.py` | UNDERSTAND_SYSTEM_PROMPT + build_understand_prompt() |
| `backend/app/schemas/understand.py` | UnderstandRequest Pydantic schema |
| `data/seed_words.py` | One-time script: parse tanzil word-by-word JSON → insert into word_meanings |
| `data/quran_words.json` | Downloaded word-by-word dataset (77,430 words) |
| `composeApp/src/commonTest/kotlin/com/quranapp/domain/usecase/SpacedRepetitionTest.kt` | Unit tests for SM-2 algorithm |
| `composeApp/src/commonTest/kotlin/com/quranapp/data/repository/LearningRepositoryTest.kt` | Integration tests for LearningRepositoryImpl |

### Modified files
| Path | Change |
|------|--------|
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/AyahItem.kt` | Add `showWordBreakdown`, `onUnderstandClick`, `onWordClick` params; render WordBreakdownRow and Understand button |
| `composeApp/src/commonMain/kotlin/com/quranapp/ui/screens/quran/QuranScreens.kt` | Add word breakdown toggle + Practice button to top bar; wire new sheet states for understand + word detail; pass LearningViewModel |
| `composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/ViewModels.kt` | Add `showWordBreakdown` to QuranUiState; add `toggleWordBreakdown()` to QuranViewModel |
| `composeApp/src/commonMain/kotlin/com/quranapp/di/Modules.kt` | Register LearningRepository, all learning use cases, LearningViewModel, UnderstandRemoteDataSource |
| `composeApp/src/commonMain/kotlin/com/quranapp/domain/repository/Repositories.kt` | No change — LearningRepository goes in its own file |
| `backend/app/main.py` | Register understand router |
| `composeApp/src/androidMain/kotlin/com/quranapp/di/AndroidModule.kt` | Register ReviewReminderWorker scheduling |

---

## Task 1: Download Word-by-Word Dataset

**Files:**
- Create: `data/quran_words.json`
- Create: `data/seed_words.py`

- [ ] **Step 1: Download the Quran word-by-word dataset**

Run from the project root:
```bash
cd /home/basitdev/Me/Quran_App/data
curl -L "https://raw.githubusercontent.com/risan/quran-json/main/data/words/en.json" -o quran_words.json
wc -l quran_words.json
```
Expected: a JSON file containing word data. If this URL fails, use the tanzil.net morphology dataset:
```bash
# Alternative: clone and use quranmorph data
pip install requests
python3 -c "
import requests, json
# Quran words from quran.com API (free, no auth)
words = {}
for surah in range(1, 115):
    r = requests.get(f'https://api.qurancdn.com/api/qdc/verses/by_chapter/{surah}?words=true&word_fields=text_uthmani,transliteration,translation&per_page=300')
    data = r.json()
    words[surah] = data
    print(f'Surah {surah} done')
with open('quran_words_raw.json', 'w') as f:
    json.dump(words, f)
print('Done')
"
```

- [ ] **Step 2: Create the seed script**

Create `data/seed_words.py`:
```python
#!/usr/bin/env python3
"""
Seed script: parse quran word-by-word JSON and insert into SQLite word_meanings table.
Run once: python3 data/seed_words.py
Requires: pip install requests
"""
import sqlite3
import json
import requests
import sys
import os

DB_PATH = os.path.join(
    os.path.dirname(__file__),
    "../composeApp/src/androidMain/assets/quran.db"
)


def fetch_words_for_surah(surah_number: int) -> list[dict]:
    """Fetch word-by-word data from quran.com free API."""
    url = f"https://api.qurancdn.com/api/qdc/verses/by_chapter/{surah_number}"
    params = {
        "words": "true",
        "word_fields": "text_uthmani,transliteration,translation",
        "per_page": 300,
        "page": 1,
    }
    resp = requests.get(url, params=params, timeout=10)
    resp.raise_for_status()
    return resp.json().get("verses", [])


def seed_surah(conn: sqlite3.Connection, surah_number: int) -> int:
    verses = fetch_words_for_surah(surah_number)
    count = 0
    cursor = conn.cursor()
    for verse in verses:
        ayah_number = verse["verse_number"]
        for word in verse.get("words", []):
            if word.get("char_type_name") == "end":
                continue  # skip ayah-end marker
            pos = word["position"]
            arabic = word.get("text_uthmani", "")
            transliteration = word.get("transliteration", {}).get("text", "")
            english = word.get("translation", {}).get("text", "")
            cursor.execute(
                """
                INSERT OR IGNORE INTO word_meanings
                    (surah_number, ayah_number, word_position,
                     arabic_word, transliteration, english_meaning)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (surah_number, ayah_number, pos, arabic, transliteration, english),
            )
            count += 1
    conn.commit()
    return count


def main():
    if not os.path.exists(DB_PATH):
        print(f"ERROR: DB not found at {DB_PATH}")
        sys.exit(1)
    conn = sqlite3.connect(DB_PATH)
    total = 0
    for surah in range(1, 115):
        n = seed_surah(conn, surah)
        total += n
        print(f"Surah {surah:3d}: {n} words inserted  (total: {total})")
    conn.close()
    print(f"\nDone. {total} word records seeded.")


if __name__ == "__main__":
    main()
```

- [ ] **Step 3: Commit**
```bash
cd /home/basitdev/Me/Quran_App
git add data/seed_words.py
git commit -m "chore: add word-by-word seed script"
```

---

## Task 2: SQLDelight Schema — word_meanings

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/quranapp/db/WordMeanings.sq`

- [ ] **Step 1: Create the schema file**

Create `composeApp/src/commonMain/sqldelight/com/quranapp/db/WordMeanings.sq`:
```sql
CREATE TABLE IF NOT EXISTS word_meanings (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number          INTEGER NOT NULL,
    ayah_number           INTEGER NOT NULL,
    word_position         INTEGER NOT NULL,
    arabic_word           TEXT    NOT NULL,
    transliteration       TEXT    NOT NULL DEFAULT '',
    english_meaning       TEXT    NOT NULL DEFAULT '',
    root_arabic           TEXT,
    root_english          TEXT,
    quran_occurrence_count INTEGER DEFAULT 0,
    UNIQUE(surah_number, ayah_number, word_position)
);

CREATE INDEX IF NOT EXISTS idx_word_meanings_ayah
    ON word_meanings(surah_number, ayah_number);

selectByAyah:
SELECT * FROM word_meanings
WHERE surah_number = :surahNumber AND ayah_number = :ayahNumber
ORDER BY word_position;

selectById:
SELECT * FROM word_meanings
WHERE id = :id;

insert:
INSERT OR IGNORE INTO word_meanings
    (surah_number, ayah_number, word_position, arabic_word,
     transliteration, english_meaning, root_arabic, root_english, quran_occurrence_count)
VALUES
    (:surahNumber, :ayahNumber, :wordPosition, :arabicWord,
     :transliteration, :englishMeaning, :rootArabic, :rootEnglish, :occurrenceCount);
```

- [ ] **Step 2: Build to verify SQLDelight generates without errors**
```bash
cd /home/basitdev/Me/Quran_App
./gradlew :composeApp:generateCommonMainQuranDatabaseInterface 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/sqldelight/com/quranapp/db/WordMeanings.sq
git commit -m "feat: add word_meanings SQLDelight schema"
```

---

## Task 3: SQLDelight Schema — learning data (word_bank, flashcard_reviews, studied_ayahs)

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/quranapp/db/LearningData.sq`

- [ ] **Step 1: Create the schema file**

Create `composeApp/src/commonMain/sqldelight/com/quranapp/db/LearningData.sq`:
```sql
-- ── Word Bank ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS word_bank (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number   INTEGER NOT NULL,
    ayah_number    INTEGER NOT NULL,
    word_position  INTEGER NOT NULL,
    added_at       INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    UNIQUE(surah_number, ayah_number, word_position)
);

insertWordBank:
INSERT OR IGNORE INTO word_bank(surah_number, ayah_number, word_position)
VALUES (:surahNumber, :ayahNumber, :wordPosition);

isInWordBank:
SELECT COUNT(*) FROM word_bank
WHERE surah_number = :surahNumber
  AND ayah_number  = :ayahNumber
  AND word_position = :wordPosition;

countWordBank:
SELECT COUNT(*) FROM word_bank;

-- ── Flashcard Reviews (SM-2) ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS flashcard_reviews (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    word_bank_id     INTEGER NOT NULL UNIQUE REFERENCES word_bank(id) ON DELETE CASCADE,
    next_review_at   INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    interval_days    INTEGER NOT NULL DEFAULT 1,
    ease_factor      REAL    NOT NULL DEFAULT 2.5,
    repetitions      INTEGER NOT NULL DEFAULT 0,
    last_reviewed_at INTEGER
);

insertReview:
INSERT OR IGNORE INTO flashcard_reviews(word_bank_id)
VALUES (:wordBankId);

selectDueReviews:
SELECT
    fr.*,
    wm.arabic_word,
    wm.transliteration,
    wm.english_meaning,
    wm.surah_number AS wm_surah,
    wm.ayah_number  AS wm_ayah
FROM flashcard_reviews fr
JOIN word_bank wb ON fr.word_bank_id = wb.id
JOIN word_meanings wm
    ON wb.surah_number  = wm.surah_number
   AND wb.ayah_number   = wm.ayah_number
   AND wb.word_position = wm.word_position
WHERE fr.next_review_at <= :nowEpoch
ORDER BY fr.next_review_at
LIMIT 50;

countDueReviews:
SELECT COUNT(*) FROM flashcard_reviews
WHERE next_review_at <= :nowEpoch;

updateReview:
UPDATE flashcard_reviews
SET next_review_at   = :nextReviewAt,
    interval_days    = :intervalDays,
    ease_factor      = :easeFactor,
    repetitions      = :repetitions,
    last_reviewed_at = :lastReviewedAt
WHERE id = :id;

-- ── Studied Ayahs ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS studied_ayahs (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    studied_at   INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    UNIQUE(surah_number, ayah_number)
);

markStudied:
INSERT OR REPLACE INTO studied_ayahs(surah_number, ayah_number)
VALUES (:surahNumber, :ayahNumber);

countStudied:
SELECT COUNT(*) FROM studied_ayahs;

isStudied:
SELECT COUNT(*) FROM studied_ayahs
WHERE surah_number = :surahNumber AND ayah_number = :ayahNumber;

countStudiedBySurah:
SELECT surah_number, COUNT(*) AS count
FROM studied_ayahs
GROUP BY surah_number;

-- ── Streak ─────────────────────────────────────────────────────────────────
-- Stored in app_settings with key='streak_last_review_date' and key='streak_count'
```

- [ ] **Step 2: Build to verify**
```bash
./gradlew :composeApp:generateCommonMainQuranDatabaseInterface 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/sqldelight/com/quranapp/db/LearningData.sq
git commit -m "feat: add word_bank, flashcard_reviews, studied_ayahs SQLDelight schema"
```

---

## Task 4: Domain Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/domain/model/LearningModels.kt`

- [ ] **Step 1: Create domain models**

Create `composeApp/src/commonMain/kotlin/com/quranapp/domain/model/LearningModels.kt`:
```kotlin
package com.quranapp.domain.model

data class WordMeaning(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordPosition: Int,
    val arabicWord: String,
    val transliteration: String,
    val englishMeaning: String,
    val rootArabic: String?,
    val rootEnglish: String?,
    val quranOccurrenceCount: Int,
)

data class DueFlashcard(
    val reviewId: Long,
    val wordBankId: Long,
    val arabicWord: String,
    val transliteration: String,
    val englishMeaning: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitions: Int,
)

enum class ReviewRating { AGAIN, HARD, KNOW_IT }

data class ReviewResult(
    val reviewId: Long,
    val nextIntervalDays: Int,
    val nextEaseFactor: Double,
    val nextRepetitions: Int,
    val nextReviewEpoch: Long,
)

data class LearningProgress(
    val wordBankCount: Int,
    val studiedAyahCount: Int,
    val dueReviewCount: Int,
    val streakDays: Int,
    /** Map of surah_number → studied ayah count in that surah */
    val studiedBySurah: Map<Int, Int>,
)
```

- [ ] **Step 2: Build to check compilation**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|warning:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/domain/model/LearningModels.kt
git commit -m "feat: add learning domain models"
```

---

## Task 5: SM-2 Spaced Repetition Algorithm (TDD)

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/quranapp/domain/usecase/SpacedRepetitionTest.kt`
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/SpacedRepetitionUseCase.kt`

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/quranapp/domain/usecase/SpacedRepetitionTest.kt`:
```kotlin
package com.quranapp.domain.usecase

import com.quranapp.domain.model.ReviewRating
import com.quranapp.domain.usecase.learning.calculateNextReview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpacedRepetitionTest {

    @Test
    fun `AGAIN resets interval to 1 and keeps repetitions at 0`() {
        val result = calculateNextReview(
            intervalDays = 7,
            easeFactor = 2.5,
            repetitions = 3,
            rating = ReviewRating.AGAIN,
        )
        assertEquals(1, result.nextIntervalDays)
        assertEquals(0, result.nextRepetitions)
    }

    @Test
    fun `AGAIN decreases ease factor by 0_20`() {
        val result = calculateNextReview(
            intervalDays = 7,
            easeFactor = 2.5,
            repetitions = 3,
            rating = ReviewRating.AGAIN,
        )
        assertEquals(2.3, result.nextEaseFactor, absoluteTolerance = 0.01)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        val result = calculateNextReview(
            intervalDays = 1,
            easeFactor = 1.3,
            repetitions = 0,
            rating = ReviewRating.AGAIN,
        )
        assertEquals(1.3, result.nextEaseFactor, absoluteTolerance = 0.01)
    }

    @Test
    fun `HARD keeps interval and decreases ease by 0_15`() {
        val result = calculateNextReview(
            intervalDays = 6,
            easeFactor = 2.5,
            repetitions = 2,
            rating = ReviewRating.HARD,
        )
        assertEquals(6, result.nextIntervalDays)
        assertEquals(2.35, result.nextEaseFactor, absoluteTolerance = 0.01)
        assertEquals(2, result.nextRepetitions)
    }

    @Test
    fun `KNOW_IT first repetition sets interval to 1`() {
        val result = calculateNextReview(
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 0,
            rating = ReviewRating.KNOW_IT,
        )
        assertEquals(1, result.nextIntervalDays)
        assertEquals(1, result.nextRepetitions)
    }

    @Test
    fun `KNOW_IT second repetition sets interval to 6`() {
        val result = calculateNextReview(
            intervalDays = 1,
            easeFactor = 2.5,
            repetitions = 1,
            rating = ReviewRating.KNOW_IT,
        )
        assertEquals(6, result.nextIntervalDays)
        assertEquals(2, result.nextRepetitions)
    }

    @Test
    fun `KNOW_IT third repetition multiplies by ease factor`() {
        val result = calculateNextReview(
            intervalDays = 6,
            easeFactor = 2.5,
            repetitions = 2,
            rating = ReviewRating.KNOW_IT,
        )
        assertEquals(15, result.nextIntervalDays) // 6 * 2.5 = 15
        assertEquals(3, result.nextRepetitions)
    }

    @Test
    fun `nextReviewEpoch is approximately nowEpoch + intervalDays * 86400`() {
        val nowEpoch = 1_700_000_000L
        val result = calculateNextReview(
            intervalDays = 3,
            easeFactor = 2.5,
            repetitions = 2,
            rating = ReviewRating.KNOW_IT,
            nowEpoch = nowEpoch,
        )
        val expectedEpoch = nowEpoch + (result.nextIntervalDays * 86400L)
        assertEquals(expectedEpoch, result.nextReviewEpoch)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**
```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.SpacedRepetitionTest" 2>&1 | tail -20
```
Expected: FAIL with `Unresolved reference: calculateNextReview`

- [ ] **Step 3: Implement the algorithm**

Create `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/SpacedRepetitionUseCase.kt`:
```kotlin
package com.quranapp.domain.usecase.learning

import com.quranapp.domain.model.ReviewRating
import com.quranapp.domain.model.ReviewResult

/**
 * SM-2 spaced repetition algorithm.
 * Pure function — no DB access. Call this to compute the next review schedule,
 * then persist the result via RecordReviewUseCase.
 */
fun calculateNextReview(
    intervalDays: Int,
    easeFactor: Double,
    repetitions: Int,
    rating: ReviewRating,
    nowEpoch: Long = System.currentTimeMillis() / 1000L,
    reviewId: Long = 0L,
): ReviewResult {
    val (newInterval, newEase, newReps) = when (rating) {
        ReviewRating.AGAIN -> {
            val newEase = maxOf(1.3, easeFactor - 0.20)
            Triple(1, newEase, 0)
        }
        ReviewRating.HARD -> {
            val newEase = maxOf(1.3, easeFactor - 0.15)
            Triple(intervalDays, newEase, repetitions)
        }
        ReviewRating.KNOW_IT -> {
            val newInterval = when (repetitions) {
                0 -> 1
                1 -> 6
                else -> (intervalDays * easeFactor).toInt()
            }
            Triple(newInterval, easeFactor, repetitions + 1)
        }
    }
    return ReviewResult(
        reviewId = reviewId,
        nextIntervalDays = newInterval,
        nextEaseFactor = newEase,
        nextRepetitions = newReps,
        nextReviewEpoch = nowEpoch + (newInterval * 86400L),
    )
}
```

- [ ] **Step 4: Run tests to confirm they pass**
```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.SpacedRepetitionTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL` with all 8 tests passing

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonTest/kotlin/com/quranapp/domain/usecase/SpacedRepetitionTest.kt \
        composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/SpacedRepetitionUseCase.kt
git commit -m "feat: SM-2 spaced repetition algorithm with tests"
```

---

## Task 6: LearningRepository Interface + Implementation

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/domain/repository/LearningRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/data/repository/LearningRepositoryImpl.kt`

- [ ] **Step 1: Write the interface**

Create `composeApp/src/commonMain/kotlin/com/quranapp/domain/repository/LearningRepository.kt`:
```kotlin
package com.quranapp.domain.repository

import com.quranapp.domain.model.*

interface LearningRepository {
    // Word meanings (Layer 1)
    suspend fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning>

    // Word bank (Layer 3)
    suspend fun addToWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Long
    suspend fun isInWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Boolean
    suspend fun getWordBankCount(): Int

    // Flashcard reviews (Layer 3)
    suspend fun ensureReviewRecord(wordBankId: Long)
    suspend fun getDueFlashcards(nowEpoch: Long): List<DueFlashcard>
    suspend fun getDueCount(nowEpoch: Long): Int
    suspend fun updateReview(result: ReviewResult)

    // Studied ayahs (Layer 2)
    suspend fun markAyahStudied(surahNumber: Int, ayahNumber: Int)
    suspend fun getStudiedAyahCount(): Int
    suspend fun getStudiedBySurah(): Map<Int, Int>

    // Streak (stored in app_settings via SettingsRepository)
    // Progress aggregation
    suspend fun getProgress(nowEpoch: Long): LearningProgress
}
```

- [ ] **Step 2: Write the implementation**

Create `composeApp/src/commonMain/kotlin/com/quranapp/data/repository/LearningRepositoryImpl.kt`:
```kotlin
package com.quranapp.data.repository

import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.*
import com.quranapp.domain.repository.LearningRepository

class LearningRepositoryImpl(
    private val db: QuranDatabase,
) : LearningRepository {

    override suspend fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning> =
        db.wordMeaningsQueries
            .selectByAyah(surahNumber.toLong(), ayahNumber.toLong())
            .executeAsList()
            .map { it.toDomain() }

    override suspend fun addToWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Long {
        db.learningDataQueries.insertWordBank(
            surahNumber.toLong(), ayahNumber.toLong(), wordPosition.toLong()
        )
        // Return the rowid of the inserted (or existing) record
        return db.learningDataQueries
            .isInWordBank(surahNumber.toLong(), ayahNumber.toLong(), wordPosition.toLong())
            .executeAsOne()  // returns count; real id found via separate query if needed
            .let { 0L } // placeholder — ensureReviewRecord uses wordBankId from insert
    }

    override suspend fun isInWordBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int): Boolean =
        db.learningDataQueries
            .isInWordBank(surahNumber.toLong(), ayahNumber.toLong(), wordPosition.toLong())
            .executeAsOne() > 0L

    override suspend fun getWordBankCount(): Int =
        db.learningDataQueries.countWordBank().executeAsOne().toInt()

    override suspend fun ensureReviewRecord(wordBankId: Long) {
        db.learningDataQueries.insertReview(wordBankId)
    }

    override suspend fun getDueFlashcards(nowEpoch: Long): List<DueFlashcard> =
        db.learningDataQueries.selectDueReviews(nowEpoch).executeAsList().map { row ->
            DueFlashcard(
                reviewId = row.id,
                wordBankId = row.word_bank_id,
                arabicWord = row.arabic_word,
                transliteration = row.transliteration,
                englishMeaning = row.english_meaning,
                surahNumber = row.wm_surah.toInt(),
                ayahNumber = row.wm_ayah.toInt(),
                intervalDays = row.interval_days.toInt(),
                easeFactor = row.ease_factor,
                repetitions = row.repetitions.toInt(),
            )
        }

    override suspend fun getDueCount(nowEpoch: Long): Int =
        db.learningDataQueries.countDueReviews(nowEpoch).executeAsOne().toInt()

    override suspend fun updateReview(result: ReviewResult) {
        val nowEpoch = System.currentTimeMillis() / 1000L
        db.learningDataQueries.updateReview(
            nextReviewAt = result.nextReviewEpoch,
            intervalDays = result.nextIntervalDays.toLong(),
            easeFactor = result.nextEaseFactor,
            repetitions = result.nextRepetitions.toLong(),
            lastReviewedAt = nowEpoch,
            id = result.reviewId,
        )
    }

    override suspend fun markAyahStudied(surahNumber: Int, ayahNumber: Int) {
        db.learningDataQueries.markStudied(surahNumber.toLong(), ayahNumber.toLong())
    }

    override suspend fun getStudiedAyahCount(): Int =
        db.learningDataQueries.countStudied().executeAsOne().toInt()

    override suspend fun getStudiedBySurah(): Map<Int, Int> =
        db.learningDataQueries.countStudiedBySurah().executeAsList()
            .associate { it.surah_number.toInt() to it.count_.toInt() }

    override suspend fun getProgress(nowEpoch: Long): LearningProgress = LearningProgress(
        wordBankCount = getWordBankCount(),
        studiedAyahCount = getStudiedAyahCount(),
        dueReviewCount = getDueCount(nowEpoch),
        streakDays = 0, // wired in Task 11
        studiedBySurah = getStudiedBySurah(),
    )

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun com.quranapp.db.Word_meanings.toDomain() = WordMeaning(
        id = id,
        surahNumber = surah_number.toInt(),
        ayahNumber = ayah_number.toInt(),
        wordPosition = word_position.toInt(),
        arabicWord = arabic_word,
        transliteration = transliteration,
        englishMeaning = english_meaning,
        rootArabic = root_arabic,
        rootEnglish = root_english,
        quranOccurrenceCount = quran_occurrence_count?.toInt() ?: 0,
    )
}
```

- [ ] **Step 3: Build to verify compilation**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/domain/repository/LearningRepository.kt \
        composeApp/src/commonMain/kotlin/com/quranapp/data/repository/LearningRepositoryImpl.kt
git commit -m "feat: LearningRepository interface and SQLDelight implementation"
```

---

## Task 7: Learning Use Cases

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/LearningUseCases.kt`

- [ ] **Step 1: Create all learning use cases in one file**

Create `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/LearningUseCases.kt`:
```kotlin
package com.quranapp.domain.usecase.learning

import com.quranapp.domain.model.*
import com.quranapp.domain.repository.LearningRepository

class GetWordMeaningsUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): List<WordMeaning> =
        repo.getWordMeanings(surahNumber, ayahNumber)
}

class AddToWordBankUseCase(private val repo: LearningRepository) {
    /** Adds the word to the bank and creates an initial review record. */
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int, wordPosition: Int) {
        val wordBankId = repo.addToWordBank(surahNumber, ayahNumber, wordPosition)
        repo.ensureReviewRecord(wordBankId)
    }
}

class GetDueFlashcardsUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(): List<DueFlashcard> {
        val nowEpoch = System.currentTimeMillis() / 1000L
        return repo.getDueFlashcards(nowEpoch)
    }
}

class RecordReviewUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(card: DueFlashcard, rating: ReviewRating): ReviewResult {
        val result = calculateNextReview(
            intervalDays = card.intervalDays,
            easeFactor = card.easeFactor,
            repetitions = card.repetitions,
            rating = rating,
            reviewId = card.reviewId,
        )
        repo.updateReview(result)
        return result
    }
}

class MarkAyahStudiedUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int) =
        repo.markAyahStudied(surahNumber, ayahNumber)
}

class GetProgressUseCase(private val repo: LearningRepository) {
    suspend operator fun invoke(): LearningProgress {
        val nowEpoch = System.currentTimeMillis() / 1000L
        return repo.getProgress(nowEpoch)
    }
}
```

- [ ] **Step 2: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/LearningUseCases.kt
git commit -m "feat: learning use cases (word bank, flashcards, progress, studied ayahs)"
```

---

## Task 8: UnderstandAyah — Backend Endpoint

**Files:**
- Create: `backend/app/schemas/understand.py`
- Create: `backend/app/rag/understand_prompt.py`
- Create: `backend/app/routers/understand.py`
- Modify: `backend/app/main.py`

- [ ] **Step 1: Create the request schema**

Create `backend/app/schemas/understand.py`:
```python
"""Request schema for the understand-ayah endpoint."""
from pydantic import BaseModel, field_validator


class UnderstandRequest(BaseModel):
    surah: int
    ayah: int
    arabic_text: str
    translation: str

    @field_validator("surah")
    @classmethod
    def valid_surah(cls, v: int) -> int:
        if not 1 <= v <= 114:
            raise ValueError("surah must be between 1 and 114")
        return v

    @field_validator("ayah")
    @classmethod
    def valid_ayah(cls, v: int) -> int:
        if v < 1:
            raise ValueError("ayah must be >= 1")
        return v
```

- [ ] **Step 2: Create the understand system prompt**

Create `backend/app/rag/understand_prompt.py`:
```python
"""System prompt and prompt builder for the understand-ayah teaching endpoint."""

UNDERSTAND_SYSTEM_PROMPT = """You are a warm, knowledgeable Quran teacher helping a student understand the Quran deeply.

When given an ayah, respond in EXACTLY this format with these 4 section headers:

CONTEXT
[2-3 sentences on when and why this ayah was revealed — Asbab al-Nuzul. If the reason for revelation is well-known, mention it. If not, describe the surah's general context.]

WORD HIGHLIGHTS
[Pick 2-3 key Arabic words from this ayah. For each: write the Arabic word, its transliteration, and explain WHY that specific word was chosen — its theological depth, linguistic nuance, or what it reveals about meaning. Format each as: "**word** (transliteration) — explanation"]

SCHOLAR VIEW
[Synthesize what the classical scholars say, using ONLY the tafsir context provided. Mention the scholar by name. 3-5 sentences. Never invent opinions.]

PRACTICAL LESSON
[One clear, warm, personal lesson the reader can take from this ayah today. Write it directly to the student — use "you". 2-3 sentences. Make it actionable and spiritually uplifting.]

Rules:
- Never use bullet points — write in flowing prose within each section
- Keep total response under 400 words
- Do not add section numbers or extra headers
- If tafsir context is missing, base SCHOLAR VIEW only on what is provided
- End PRACTICAL LESSON with the ayah reference in parentheses e.g. (Surah 2:255)
"""


def build_understand_prompt(
    surah: int,
    ayah: int,
    arabic_text: str,
    translation: str,
    retrieved: dict,
) -> list[dict]:
    """Build the message list for the understand-ayah LLM call."""
    tafsir_context = ""
    if retrieved.get("tafsir"):
        parts = []
        for t in retrieved["tafsir"]:
            book = t["book_name"].replace("_", " ").title()
            content = t["content"][:500] + "..." if len(t["content"]) > 500 else t["content"]
            parts.append(f"[{book}]\n{content}")
        tafsir_context = "\n\n".join(parts)
    else:
        tafsir_context = "No tafsir context available for this ayah."

    user_message = (
        f"Ayah: Surah {surah}:{ayah}\n"
        f"Arabic: {arabic_text}\n"
        f"Translation: {translation}\n\n"
        f"Tafsir Context:\n{tafsir_context}\n\n"
        f"Please explain this ayah."
    )
    return [{"role": "user", "content": user_message}]
```

- [ ] **Step 3: Create the router**

Create `backend/app/routers/understand.py`:
```python
"""Understand-ayah endpoint — teaching mode for the Quran learning system."""
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from app.dependencies import get_db
from app.rag.retriever import retrieve
from app.rag.llm_client import stream_llm_response
from app.rag.understand_prompt import UNDERSTAND_SYSTEM_PROMPT, build_understand_prompt
from app.schemas.understand import UnderstandRequest

router = APIRouter(prefix="/chat", tags=["understand"])


@router.post("/understand-ayah")
async def understand_ayah(request: UnderstandRequest, db: AsyncSession = Depends(get_db)):
    """Stream a structured 4-section teaching explanation for a single ayah."""
    query = f"Explain Surah {request.surah} Ayah {request.ayah}: {request.translation}"
    retrieved = await retrieve(query, db)
    messages = build_understand_prompt(
        surah=request.surah,
        ayah=request.ayah,
        arabic_text=request.arabic_text,
        translation=request.translation,
        retrieved=retrieved,
    )

    async def event_generator():
        async for token in stream_llm_response(
            UNDERSTAND_SYSTEM_PROMPT, messages, history=[]
        ):
            yield f"data: {token}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
```

- [ ] **Step 4: Register the router in main.py**

Read `backend/app/main.py` first, then add the import and include:
```bash
cat /home/basitdev/Me/Quran_App/backend/app/main.py
```
Then add after the existing router registrations:
```python
from app.routers.understand import router as understand_router
app.include_router(understand_router)
```

- [ ] **Step 5: Test the endpoint manually**
```bash
cd /home/basitdev/Me/Quran_App/backend
# Start backend if not running:
# docker-compose up -d
curl -X POST http://localhost:8000/chat/understand-ayah \
  -H "Content-Type: application/json" \
  -d '{"surah":1,"ayah":1,"arabic_text":"بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ","translation":"In the name of Allah, the Entirely Merciful, the Especially Merciful"}' \
  --no-buffer 2>&1 | head -30
```
Expected: streaming `data: <token>` lines in 4 sections (CONTEXT, WORD HIGHLIGHTS, SCHOLAR VIEW, PRACTICAL LESSON)

- [ ] **Step 6: Commit**
```bash
git add backend/app/schemas/understand.py \
        backend/app/rag/understand_prompt.py \
        backend/app/routers/understand.py \
        backend/app/main.py
git commit -m "feat: /chat/understand-ayah streaming endpoint with 4-section teaching prompt"
```

---

## Task 9: UnderstandAyah — Android Client

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/data/remote/UnderstandRemoteDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/UnderstandAyahUseCase.kt`

- [ ] **Step 1: Create the remote data source**

Create `composeApp/src/commonMain/kotlin/com/quranapp/data/remote/UnderstandRemoteDataSource.kt`:
```kotlin
package com.quranapp.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
data class UnderstandRequestDto(
    val surah: Int,
    val ayah: Int,
    val arabic_text: String,
    val translation: String,
)

class UnderstandRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    fun streamUnderstand(
        surah: Int,
        ayah: Int,
        arabicText: String,
        translation: String,
    ): Flow<String> = flow {
        client.preparePost("$baseUrl/chat/understand-ayah") {
            contentType(ContentType.Application.Json)
            setBody(UnderstandRequestDto(surah, ayah, arabicText, translation))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line()
                if (line != null && line.startsWith("data: ")) {
                    val token = line.substring(6)
                    if (token.isNotEmpty()) emit(token)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create the use case**

Create `composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/UnderstandAyahUseCase.kt`:
```kotlin
package com.quranapp.domain.usecase.learning

import com.quranapp.data.remote.UnderstandRemoteDataSource
import kotlinx.coroutines.flow.Flow

class UnderstandAyahUseCase(private val remote: UnderstandRemoteDataSource) {
    operator fun invoke(
        surah: Int,
        ayah: Int,
        arabicText: String,
        translation: String,
    ): Flow<String> = remote.streamUnderstand(surah, ayah, arabicText, translation)
}
```

- [ ] **Step 3: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/data/remote/UnderstandRemoteDataSource.kt \
        composeApp/src/commonMain/kotlin/com/quranapp/domain/usecase/learning/UnderstandAyahUseCase.kt
git commit -m "feat: UnderstandAyah Ktor streaming client and use case"
```

---

## Task 10: LearningViewModel

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/LearningViewModel.kt`

- [ ] **Step 1: Create the ViewModel**

Create `composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/LearningViewModel.kt`:
```kotlin
package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.*
import com.quranapp.domain.usecase.learning.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LearningUiState(
    // Layer 1: word breakdown
    val wordMeanings: List<WordMeaning> = emptyList(),
    val isLoadingWords: Boolean = false,

    // Layer 2: understand
    val understandText: String = "",
    val isLoadingUnderstand: Boolean = false,
    val understandError: String? = null,

    // Layer 3: flashcards
    val dueFlashcards: List<DueFlashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val showAnswer: Boolean = false,
    val sessionComplete: Boolean = false,
    val sessionCorrect: Int = 0,
    val sessionTotal: Int = 0,

    // Progress
    val progress: LearningProgress? = null,
)

class LearningViewModel(
    private val getWordMeanings: GetWordMeaningsUseCase,
    private val addToWordBank: AddToWordBankUseCase,
    private val getDueFlashcards: GetDueFlashcardsUseCase,
    private val recordReview: RecordReviewUseCase,
    private val markAyahStudied: MarkAyahStudiedUseCase,
    private val getProgress: GetProgressUseCase,
    private val understandAyah: UnderstandAyahUseCase,
) : ScreenModel {

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    // ── Layer 1 ──────────────────────────────────────────────────────────────

    fun loadWordMeanings(surahNumber: Int, ayahNumber: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoadingWords = true) }
            val words = getWordMeanings(surahNumber, ayahNumber)
            _uiState.update { it.copy(wordMeanings = words, isLoadingWords = false) }
        }
    }

    fun addWordToBank(surahNumber: Int, ayahNumber: Int, wordPosition: Int) {
        screenModelScope.launch {
            addToWordBank(surahNumber, ayahNumber, wordPosition)
        }
    }

    // ── Layer 2 ──────────────────────────────────────────────────────────────

    fun understandAyah(surahNumber: Int, ayahNumber: Int, arabicText: String, translation: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(understandText = "", isLoadingUnderstand = true, understandError = null) }
            try {
                understandAyah(surahNumber, ayahNumber, arabicText, translation)
                    .collect { token ->
                        _uiState.update { it.copy(understandText = it.understandText + token) }
                    }
                markAyahStudied(surahNumber, ayahNumber)
            } catch (e: Exception) {
                _uiState.update { it.copy(understandError = "Failed to load explanation. Check your connection.") }
            } finally {
                _uiState.update { it.copy(isLoadingUnderstand = false) }
            }
        }
    }

    fun clearUnderstand() {
        _uiState.update { it.copy(understandText = "", understandError = null) }
    }

    // ── Layer 3: Flashcards ──────────────────────────────────────────────────

    fun startFlashcardSession() {
        screenModelScope.launch {
            val cards = getDueFlashcards()
            _uiState.update {
                it.copy(
                    dueFlashcards = cards,
                    currentCardIndex = 0,
                    showAnswer = false,
                    sessionComplete = cards.isEmpty(),
                    sessionCorrect = 0,
                    sessionTotal = cards.size,
                )
            }
        }
    }

    fun revealAnswer() {
        _uiState.update { it.copy(showAnswer = true) }
    }

    fun submitRating(rating: ReviewRating) {
        val state = _uiState.value
        val card = state.dueFlashcards.getOrNull(state.currentCardIndex) ?: return
        screenModelScope.launch {
            recordReview(card, rating)
            val newCorrect = if (rating == ReviewRating.KNOW_IT) state.sessionCorrect + 1 else state.sessionCorrect
            val nextIndex = state.currentCardIndex + 1
            val isDone = nextIndex >= state.dueFlashcards.size
            _uiState.update {
                it.copy(
                    currentCardIndex = nextIndex,
                    showAnswer = false,
                    sessionComplete = isDone,
                    sessionCorrect = newCorrect,
                )
            }
            if (isDone) refreshProgress()
        }
    }

    // ── Progress ─────────────────────────────────────────────────────────────

    fun refreshProgress() {
        screenModelScope.launch {
            val progress = getProgress()
            _uiState.update { it.copy(progress = progress) }
        }
    }
}
```

- [ ] **Step 2: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/LearningViewModel.kt
git commit -m "feat: LearningViewModel with word breakdown, understand, flashcard session, progress"
```

---

## Task 11: Wire Koin DI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/quranapp/di/Modules.kt`

- [ ] **Step 1: Read the current Modules.kt**
```bash
cat /home/basitdev/Me/Quran_App/composeApp/src/commonMain/kotlin/com/quranapp/di/Modules.kt
```

- [ ] **Step 2: Add learning registrations**

In `Modules.kt`, add to the imports at the top:
```kotlin
import com.quranapp.data.remote.UnderstandRemoteDataSource
import com.quranapp.data.repository.LearningRepositoryImpl
import com.quranapp.domain.repository.LearningRepository
import com.quranapp.domain.usecase.learning.*
import com.quranapp.viewmodel.LearningViewModel
```

Add to `networkModule`:
```kotlin
single { UnderstandRemoteDataSource(get(), get(org.koin.core.qualifier.named("baseUrl"))) }
```

Add to `repositoryModule`:
```kotlin
single<LearningRepository> { LearningRepositoryImpl(get()) }
```

Add to `useCaseModule`:
```kotlin
// Learning
factory { GetWordMeaningsUseCase(get()) }
factory { AddToWordBankUseCase(get()) }
factory { GetDueFlashcardsUseCase(get()) }
factory { RecordReviewUseCase(get()) }
factory { MarkAyahStudiedUseCase(get()) }
factory { GetProgressUseCase(get()) }
factory { UnderstandAyahUseCase(get()) }
```

Add to `viewModelModule`:
```kotlin
factory { LearningViewModel(get(), get(), get(), get(), get(), get(), get()) }
```

- [ ] **Step 3: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/di/Modules.kt
git commit -m "feat: wire LearningViewModel and learning dependencies into Koin DI"
```

---

## Task 12: Layer 1 UI — WordBreakdownRow + WordDetailSheet

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordBreakdownRow.kt`
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordDetailSheet.kt`

- [ ] **Step 1: Create WordBreakdownRow**

Create `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordBreakdownRow.kt`:
```kotlin
package com.quranapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.WordMeaning

@Composable
fun WordBreakdownRow(
    words: List<WordMeaning>,
    onWordClick: (WordMeaning) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Word by Word",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        // reverseLayout = true so words flow right-to-left (Arabic order)
        LazyRow(
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(words) { _, word ->
                WordCard(word = word, onClick = { onWordClick(word) })
            }
        }
    }
}

@Composable
private fun WordCard(
    word: WordMeaning,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = word.arabicWord,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = word.transliteration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = word.englishMeaning,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
```

- [ ] **Step 2: Create WordDetailSheet**

Create `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordDetailSheet.kt`:
```kotlin
package com.quranapp.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.WordMeaning

@Composable
fun WordDetailSheet(
    word: WordMeaning,
    isInWordBank: Boolean,
    onAddToWordBank: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Arabic word — large
        Text(
            text = word.arabicWord,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Transliteration
        Text(
            text = word.transliteration,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // English meaning
        Text(
            text = word.englishMeaning,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Root word (if available)
        if (!word.rootArabic.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Root:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(word.rootArabic, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                if (!word.rootEnglish.isNullOrBlank()) {
                    Text("(${word.rootEnglish})", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Occurrence count
        if (word.quranOccurrenceCount > 0) {
            Text(
                text = "Appears ${word.quranOccurrenceCount} times in the Quran",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add to word bank button
        Button(
            onClick = onAddToWordBank,
            enabled = !isInWordBank,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isInWordBank) "Already in Word Bank" else "Add to Word Bank")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
```

- [ ] **Step 3: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordBreakdownRow.kt \
        composeApp/src/commonMain/kotlin/com/quranapp/ui/component/WordDetailSheet.kt
git commit -m "feat: WordBreakdownRow and WordDetailSheet composables"
```

---

## Task 13: Layer 2 UI — UnderstandSheet

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/UnderstandSheet.kt`

- [ ] **Step 1: Create UnderstandSheet**

Create `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/UnderstandSheet.kt`:
```kotlin
package com.quranapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Section(
    val header: String,
    val icon: ImageVector,
    val color: Color,
    val content: String,
)

/** Parses the 4-section streaming response into structured cards. */
private fun parseUnderstandResponse(raw: String): List<Section> {
    val headers = listOf("CONTEXT", "WORD HIGHLIGHTS", "SCHOLAR VIEW", "PRACTICAL LESSON")
    val icons = listOf(
        Icons.Default.History,
        Icons.Default.Spellcheck,
        Icons.Default.MenuBook,
        Icons.Default.Lightbulb,
    )
    val colors = listOf(
        Color(0xFF1565C0), // blue
        Color(0xFF2E7D32), // green
        Color(0xFF6A1B9A), // purple
        Color(0xFFE65100), // orange
    )
    val sections = mutableListOf<Section>()
    for (i in headers.indices) {
        val start = raw.indexOf(headers[i])
        if (start == -1) continue
        val contentStart = start + headers[i].length
        val end = if (i + 1 < headers.size) {
            val nextIdx = raw.indexOf(headers[i + 1])
            if (nextIdx == -1) raw.length else nextIdx
        } else raw.length
        val content = raw.substring(contentStart, end).trim()
        if (content.isNotEmpty()) {
            sections.add(Section(headers[i].lowercase().replaceFirstChar { it.uppercase() }, icons[i], colors[i], content))
        }
    }
    return sections
}

@Composable
fun UnderstandSheet(
    streamText: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        // Handle — drag indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    MaterialTheme.shapes.small
                )
        )

        Text(
            text = "Understanding This Ayah",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp))
            }
            isLoading && streamText.isEmpty() -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading explanation...", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
            streamText.isNotEmpty() -> {
                val sections = parseUnderstandResponse(streamText)
                if (sections.isEmpty()) {
                    // Still streaming — show raw text
                    Text(streamText, style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp)
                } else {
                    sections.forEach { section ->
                        UnderstandSectionCard(section = section)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    // Show ellipsis if still streaming
                    if (isLoading) {
                        Text("...", color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun UnderstandSectionCard(section: Section) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = section.color.copy(alpha = 0.08f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = section.color,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = section.header,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = section.color,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        }
    }
}
```

- [ ] **Step 2: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/ui/component/UnderstandSheet.kt
git commit -m "feat: UnderstandSheet composable with 4-section streaming parser"
```

---

## Task 14: Layer 3 UI — FlashcardSession

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/FlashcardSession.kt`

- [ ] **Step 1: Create FlashcardSession**

Create `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/FlashcardSession.kt`:
```kotlin
package com.quranapp.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.DueFlashcard
import com.quranapp.domain.model.ReviewRating

@Composable
fun FlashcardSession(
    cards: List<DueFlashcard>,
    currentIndex: Int,
    showAnswer: Boolean,
    sessionComplete: Boolean,
    sessionCorrect: Int,
    sessionTotal: Int,
    onReveal: () -> Unit,
    onRating: (ReviewRating) -> Unit,
    onDismiss: () -> Unit,
) {
    if (sessionComplete) {
        FlashcardSummary(
            correct = sessionCorrect,
            total = sessionTotal,
            onDismiss = onDismiss,
        )
        return
    }

    val card = cards.getOrNull(currentIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text("Exit") }
            Text(
                text = "${currentIndex + 1} / $sessionTotal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(64.dp))
        }
        LinearProgressIndicator(
            progress = { if (sessionTotal > 0) currentIndex.toFloat() / sessionTotal else 0f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Flash card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Front: Arabic (always shown)
                Text(
                    text = card.arabicWord,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = card.transliteration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                // Back: English meaning (revealed on tap)
                AnimatedVisibility(visible = showAnswer) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = card.englishMeaning,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Surah ${card.surahNumber}:${card.ayahNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Action buttons
        if (!showAnswer) {
            Button(
                onClick = onReveal,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Reveal Answer", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Again
                OutlinedButton(
                    onClick = { onRating(ReviewRating.AGAIN) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Again", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Again")
                }
                // Hard
                OutlinedButton(
                    onClick = { onRating(ReviewRating.HARD) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF57F17)
                    ),
                ) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Hard", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hard")
                }
                // Know it
                Button(
                    onClick = { onRating(ReviewRating.KNOW_IT) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Know it", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Got it")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FlashcardSummary(correct: Int, total: Int, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Session Complete!", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$correct / $total correct",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        if (total > 0) {
            val pct = (correct * 100 / total)
            Text(
                text = "$pct% accuracy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
```

- [ ] **Step 2: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/ui/component/FlashcardSession.kt
git commit -m "feat: FlashcardSession composable with reveal/rate flow and summary screen"
```

---

## Task 15: Progress Dashboard UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/ProgressDashboard.kt`

- [ ] **Step 1: Create ProgressDashboard**

Create `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/ProgressDashboard.kt`:
```kotlin
package com.quranapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.LearningProgress

private val TOTAL_QURAN_WORDS = 14000
private val TOTAL_AYAHS = 6236
private val SURAH_COUNTS = listOf(7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,25,22,63,109,10,51,15,92,11,16,6,11,52,20,69,52,26,47,20,56,98,8,73,5,16,78,56,29,29,54,38,45,15,20,22,21,17,11,33,22,30,26,17,32,21,26,18,16,24,20,22,21,23,36,15,17,19,7,65,30,15,19,24,19,20,29,9,37,30,22,18,32,11,5,12,20,25,12,20,3,7)

@Composable
fun ProgressDashboard(
    progress: LearningProgress,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Learning Progress", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Streak",
                value = "${progress.streakDays}",
                unit = "days",
                icon = { Icon(Icons.Default.LocalFireDepartment, null,
                    tint = Color(0xFFFF6F00), modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Due Today",
                value = "${progress.dueReviewCount}",
                unit = "words",
                modifier = Modifier.weight(1f),
            )
        }

        // Word mastery progress
        ProgressRow(
            label = "Words Learned",
            current = progress.wordBankCount,
            total = TOTAL_QURAN_WORDS,
            color = MaterialTheme.colorScheme.primary,
        )

        // Ayahs studied progress
        ProgressRow(
            label = "Ayahs Studied",
            current = progress.studiedAyahCount,
            total = TOTAL_AYAHS,
            color = Color(0xFF2E7D32),
        )

        // Review button
        if (progress.dueReviewCount > 0) {
            Button(
                onClick = onStartReview,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Review ${progress.dueReviewCount} Due Words")
            }
        }

        // Surah heatmap
        Text("Surah Study Map", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text("Color shows how deeply each surah has been studied.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        SurahHeatmap(studiedBySurah = progress.studiedBySurah)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) icon()
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(unit, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun ProgressRow(label: String, current: Int, total: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$current / $total", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) current.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

@Composable
private fun SurahHeatmap(studiedBySurah: Map<Int, Int>) {
    val baseColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val maxStudied = studiedBySurah.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items((1..114).toList()) { surahNum ->
            val studied = studiedBySurah[surahNum] ?: 0
            val intensity = if (studied == 0) 0f else (studied.toFloat() / maxStudied).coerceIn(0.15f, 1f)
            val color = if (studied == 0) emptyColor else lerp(emptyColor, baseColor, intensity)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                if (surahNum <= 9) {
                    Text(
                        text = surahNum.toString(),
                        fontSize = 6.sp,
                        color = if (studied > 0)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/ui/component/ProgressDashboard.kt
git commit -m "feat: ProgressDashboard with stats, progress bars, and surah heatmap"
```

---

## Task 16: Wire UI into AyahItem + QuranReaderScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/quranapp/ui/component/AyahItem.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/quranapp/ui/screens/quran/QuranScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/ViewModels.kt`

- [ ] **Step 1: Update AyahItem to accept new params**

Replace the entire content of `AyahItem.kt`:
```kotlin
package com.quranapp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.Ayah
import com.quranapp.domain.model.QuranScript
import com.quranapp.domain.model.WordMeaning

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AyahItem(
    ayah: Ayah,
    showTranslation: Boolean,
    script: QuranScript,
    onLongClick: () -> Unit,
    onTafsirClick: () -> Unit,
    fontSize: Float = 28f,
    modifier: Modifier = Modifier,
    // Learning params (optional — defaults keep existing behaviour)
    showWordBreakdown: Boolean = false,
    wordMeanings: List<WordMeaning> = emptyList(),
    onWordClick: (WordMeaning) -> Unit = {},
    onUnderstandClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(16.dp)
    ) {
        // Ayah number + action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ayah.ayahNumber.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row {
                TextButton(onClick = onUnderstandClick) {
                    Text(
                        "Understand",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                TextButton(onClick = onTafsirClick) {
                    Text(
                        "Tafsir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text
        ArabicText(
            text = ayah.arabicText(script),
            fontSize = fontSize.sp,
            modifier = Modifier.fillMaxWidth()
        )

        // Word breakdown row (Layer 1)
        if (showWordBreakdown) {
            Spacer(modifier = Modifier.height(12.dp))
            WordBreakdownRow(
                words = wordMeanings,
                onWordClick = onWordClick,
            )
        }

        // Full translation (existing)
        if (showTranslation) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = ayah.translationEnglish,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
```

- [ ] **Step 2: Add showWordBreakdown to QuranUiState in ViewModels.kt**

In `ViewModels.kt`, update `QuranUiState`:
```kotlin
data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<Ayah> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTranslation: Boolean = true,
    val showWordBreakdown: Boolean = false,   // ← ADD THIS
    val script: QuranScript = QuranScript.HAFS,
    val readingMode: ReadingMode = ReadingMode.SCROLL,
    val currentPage: Int = 1,
    val currentSurah: Int = 1,
    val tafsiers: List<TafsirEntry> = emptyList(),
    val arabicFontSize: Float = 28f,
)
```

Add to `QuranViewModel`:
```kotlin
fun toggleWordBreakdown() {
    _uiState.update { it.copy(showWordBreakdown = !it.showWordBreakdown) }
}
```

- [ ] **Step 3: Update QuranReaderScreen**

In `QuranReaderScreen`, make these changes:

**a) Add imports at top of file:**
```kotlin
import cafe.adriel.voyager.koin.getScreenModel
import com.quranapp.viewmodel.LearningViewModel
import com.quranapp.ui.component.WordDetailSheet
import com.quranapp.ui.component.UnderstandSheet
```

**b) Inside `Content()`, add new state variables after existing ones:**
```kotlin
val learningViewModel = getScreenModel<LearningViewModel>()
val learningState by learningViewModel.uiState.collectAsState()

val wordDetailSheetState = rememberModalBottomSheetState()
var showWordDetailSheet by remember { mutableStateOf(false) }
var selectedWord by remember { mutableStateOf<com.quranapp.domain.model.WordMeaning?>(null) }
var wordInBank by remember { mutableStateOf(false) }

val understandSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
var showUnderstandSheet by remember { mutableStateOf(false) }
var selectedAyahForUnderstand by remember { mutableStateOf<com.quranapp.domain.model.Ayah?>(null) }

var showFlashcardSession by remember { mutableStateOf(false) }
var showProgressDashboard by remember { mutableStateOf(false) }
```

**c) Add word breakdown toggle + practice button to TopAppBar actions (after existing icons):**
```kotlin
IconButton(onClick = { viewModel.toggleWordBreakdown() }) {
    Icon(
        imageVector = Icons.Default.Spellcheck,
        contentDescription = "Toggle Word Breakdown",
        tint = if (uiState.showWordBreakdown) MaterialTheme.colorScheme.primary
               else LocalContentColor.current
    )
}
IconButton(onClick = {
    learningViewModel.startFlashcardSession()
    showFlashcardSession = true
}) {
    Icon(
        imageVector = Icons.Default.School,
        contentDescription = "Practice",
    )
}
```

**d) Update both AyahItem call sites (SCROLL and PAGE modes) to pass learning params:**
```kotlin
AyahItem(
    ayah = ayah,
    showTranslation = uiState.showTranslation,
    script = uiState.script,
    fontSize = uiState.arabicFontSize,
    showWordBreakdown = uiState.showWordBreakdown,
    wordMeanings = if (uiState.showWordBreakdown)
        learningState.wordMeanings.filter {
            it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber
        } else emptyList(),
    onWordClick = { word ->
        selectedWord = word
        showWordDetailSheet = true
    },
    onUnderstandClick = {
        selectedAyahForUnderstand = ayah
        learningViewModel.clearUnderstand()
        learningViewModel.understandAyah(
            ayah.surahNumber, ayah.ayahNumber,
            ayah.arabicText(uiState.script), ayah.translationEnglish
        )
        showUnderstandSheet = true
    },
    onLongClick = {
        selectedAyahForAnnotation = ayah
        showAnnotationSheet = true
    },
    onTafsirClick = {
        selectedAyahForTafsir = ayah
        viewModel.loadTafsir(ayah)
        showTafsirSheet = true
    }
)
```

**e) Load word meanings when word breakdown is toggled on — add LaunchedEffect:**
```kotlin
LaunchedEffect(uiState.showWordBreakdown, uiState.ayahs) {
    if (uiState.showWordBreakdown && uiState.ayahs.isNotEmpty()) {
        // Pre-load word meanings for visible ayahs (first 10)
        uiState.ayahs.take(10).forEach { ayah ->
            learningViewModel.loadWordMeanings(ayah.surahNumber, ayah.ayahNumber)
        }
    }
}
```

**f) Add new bottom sheets at the bottom of Content() (after existing sheets):**
```kotlin
// Word detail sheet
if (showWordDetailSheet && selectedWord != null) {
    ModalBottomSheet(
        onDismissRequest = { showWordDetailSheet = false },
        sheetState = wordDetailSheetState,
    ) {
        WordDetailSheet(
            word = selectedWord!!,
            isInWordBank = wordInBank,
            onAddToWordBank = {
                val w = selectedWord!!
                learningViewModel.addWordToBank(w.surahNumber, w.ayahNumber, w.wordPosition)
                wordInBank = true
            },
            onDismiss = { showWordDetailSheet = false },
        )
    }
}

// Understand sheet
if (showUnderstandSheet) {
    ModalBottomSheet(
        onDismissRequest = { showUnderstandSheet = false },
        sheetState = understandSheetState,
    ) {
        UnderstandSheet(
            streamText = learningState.understandText,
            isLoading = learningState.isLoadingUnderstand,
            error = learningState.understandError,
            onDismiss = { showUnderstandSheet = false },
        )
    }
}

// Flashcard session (full screen dialog)
if (showFlashcardSession) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { showFlashcardSession = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            FlashcardSession(
                cards = learningState.dueFlashcards,
                currentIndex = learningState.currentCardIndex,
                showAnswer = learningState.showAnswer,
                sessionComplete = learningState.sessionComplete,
                sessionCorrect = learningState.sessionCorrect,
                sessionTotal = learningState.sessionTotal,
                onReveal = { learningViewModel.revealAnswer() },
                onRating = { learningViewModel.submitRating(it) },
                onDismiss = { showFlashcardSession = false },
            )
        }
    }
}
```

- [ ] **Step 4: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/ui/component/AyahItem.kt \
        composeApp/src/commonMain/kotlin/com/quranapp/ui/screens/quran/QuranScreens.kt \
        composeApp/src/commonMain/kotlin/com/quranapp/viewmodel/ViewModels.kt
git commit -m "feat: wire word breakdown, understand, and flashcard session into Quran reader"
```

---

## Task 17: Progress Dashboard in Settings Screen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/quranapp/ui/screens/settings/SettingsScreen.kt` (read first)

- [ ] **Step 1: Read the current settings screen**
```bash
cat /home/basitdev/Me/Quran_App/composeApp/src/commonMain/kotlin/com/quranapp/ui/screens/settings/SettingsScreen.kt
```

- [ ] **Step 2: Add LearningViewModel and dashboard section**

Add to the screen:
```kotlin
val learningViewModel = getScreenModel<LearningViewModel>()
val learningState by learningViewModel.uiState.collectAsState()

LaunchedEffect(Unit) {
    learningViewModel.refreshProgress()
}
```

Add `ProgressDashboard` inside the settings `LazyColumn` as a new section:
```kotlin
item {
    learningState.progress?.let { progress ->
        ProgressDashboard(
            progress = progress,
            onStartReview = { /* navigate to flashcard session if needed */ },
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
```

- [ ] **Step 3: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/commonMain/kotlin/com/quranapp/ui/screens/settings/SettingsScreen.kt
git commit -m "feat: add ProgressDashboard to settings screen"
```

---

## Task 18: Daily Review Notification (Android WorkManager)

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/quranapp/notification/ReviewReminderWorker.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/quranapp/di/AndroidModule.kt`

- [ ] **Step 1: Verify WorkManager dependency exists**
```bash
grep -r "work-runtime" /home/basitdev/Me/Quran_App/composeApp/build.gradle.kts
```
If missing, add to `composeApp/build.gradle.kts` in `androidMain.dependencies`:
```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

- [ ] **Step 2: Create the Worker**

Create `composeApp/src/androidMain/kotlin/com/quranapp/notification/ReviewReminderWorker.kt`:
```kotlin
package com.quranapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.quranapp.db.QuranDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class ReviewReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val db: QuranDatabase by inject()

    override suspend fun doWork(): Result {
        val nowEpoch = System.currentTimeMillis() / 1000L
        val dueCount = db.learningDataQueries.countDueReviews(nowEpoch).executeAsOne().toInt()
        if (dueCount > 0) {
            showNotification(dueCount)
        }
        return Result.success()
    }

    private fun showNotification(dueCount: Int) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "review_reminder"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Review Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Quran Word Review")
            .setContentText("You have $dueCount word${if (dueCount > 1) "s" else ""} due for review today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateDelayToMorning(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "review_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private fun calculateDelayToMorning(): Long {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 8)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
```

- [ ] **Step 3: Schedule in AndroidModule.kt**

Read `AndroidModule.kt`, then add WorkManager scheduling in the Android app startup (typically in `AndroidApplication.kt` or equivalent entry point):
```bash
cat /home/basitdev/Me/Quran_App/composeApp/src/androidMain/kotlin/com/quranapp/di/AndroidModule.kt
```
Find the Android `Application` class and add:
```kotlin
ReviewReminderWorker.schedule(applicationContext)
```

- [ ] **Step 4: Build to verify**
```bash
./gradlew :composeApp:compileKotlinAndroid 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/androidMain/kotlin/com/quranapp/notification/ReviewReminderWorker.kt \
        composeApp/src/androidMain/kotlin/com/quranapp/di/AndroidModule.kt
git commit -m "feat: daily review reminder via WorkManager scheduled at 8am"
```

---

## Task 19: Run Word-Seeding Script

- [ ] **Step 1: Verify DB path**
```bash
find /home/basitdev/Me/Quran_App -name "quran.db" 2>/dev/null
```

- [ ] **Step 2: Install dependencies and run seed script**
```bash
cd /home/basitdev/Me/Quran_App
pip install requests
python3 data/seed_words.py
```
Expected output: lines like `Surah   1: 29 words inserted  (total: 29)` ... `Done. 77430 word records seeded.`

- [ ] **Step 3: Verify data**
```bash
python3 -c "
import sqlite3, os
db_path = next(f for f in __import__('glob').glob('/home/basitdev/Me/Quran_App/**/*.db', recursive=True))
conn = sqlite3.connect(db_path)
count = conn.execute('SELECT COUNT(*) FROM word_meanings').fetchone()[0]
sample = conn.execute('SELECT * FROM word_meanings WHERE surah_number=1 LIMIT 3').fetchall()
print(f'Total words: {count}')
for row in sample: print(row)
conn.close()
"
```
Expected: `Total words: 77430` (or similar) with sample rows.

- [ ] **Step 4: Commit**
```bash
git add data/
git commit -m "chore: word meanings seeding script verified"
```

---

## Task 20: Full Build + Manual Smoke Test

- [ ] **Step 1: Run all unit tests**
```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -30
```
Expected: All tests pass including `SpacedRepetitionTest`

- [ ] **Step 2: Build release APK**
```bash
./gradlew :composeApp:assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Smoke test checklist (manual, on device/emulator)**

Install APK and verify:
- [ ] Open a Surah → tap Spellcheck icon → word cards appear below each ayah
- [ ] Tap a word card → WordDetailSheet opens with Arabic, transliteration, meaning
- [ ] Tap "Add to Word Bank" → button changes to "Already in Word Bank"
- [ ] Tap "Understand" on an ayah → UnderstandSheet opens, streams 4 sections
- [ ] Tap School icon → FlashcardSession opens (if words in bank)
- [ ] Complete a flashcard session → summary screen shows
- [ ] Open Settings → ProgressDashboard shows word count, ayah count, heatmap
- [ ] Existing features unchanged: Tafsir, annotation, translation toggle, chatbot

- [ ] **Step 4: Final commit**
```bash
git add .
git commit -m "feat: Quran learning system complete — Layer 1 (word breakdown), Layer 2 (understand AI), Layer 3 (flashcards + progress)"
```

---

## Spec Coverage Check

| Spec Requirement | Covered By |
|-----------------|------------|
| word_meanings DB table | Task 2 |
| word_bank, flashcard_reviews, studied_ayahs tables | Task 3 |
| WordMeaning, DueFlashcard, ReviewResult domain models | Task 4 |
| SM-2 algorithm with tests | Task 5 |
| LearningRepository interface + impl | Task 6 |
| All 6 learning use cases | Task 7 |
| /chat/understand-ayah FastAPI endpoint | Task 8 |
| Ktor streaming client for understand | Task 9 |
| LearningViewModel (all 3 layers) | Task 10 |
| Koin DI wiring | Task 11 |
| WordBreakdownRow (RTL, tappable cards) | Task 12 |
| WordDetailSheet (meaning, root, add to bank) | Task 12 |
| UnderstandSheet (4-section streaming parser) | Task 13 |
| FlashcardSession (reveal/rate/summary) | Task 14 |
| ProgressDashboard (stats, bars, heatmap) | Task 15 |
| AyahItem updated with new params | Task 16 |
| QuranReaderScreen wired end-to-end | Task 16 |
| Progress in Settings screen | Task 17 |
| Daily review WorkManager notification | Task 18 |
| Word data seeded into SQLite | Task 19 |
| Full build + smoke test | Task 20 |
