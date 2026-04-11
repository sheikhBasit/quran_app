# Quran Learning System — Design Spec
**Date:** 2026-04-11  
**Status:** Approved  
**Approach:** A — Phased Feature Layers

---

## Overview

Transform the existing Quran app from a reader/reference tool into a genuine understanding and learning platform. Three self-contained layers integrate directly into the existing Quran reader — no new tab, no context switching. Each layer ships independently and adds value on its own.

**Goal:** A user reading any ayah can instantly understand every word, get a structured AI explanation, and build long-term vocabulary retention — all without leaving the reader.

---

## Layer 1: Word-by-Word Breakdown (Teaching Mode)

### Purpose
Allow users to understand the meaning of individual Arabic words in context, including transliteration and root word data.

### Data

**New SQLite table:**
```sql
CREATE TABLE IF NOT EXISTS word_meanings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    word_position INTEGER NOT NULL,
    arabic_word TEXT NOT NULL,
    transliteration TEXT NOT NULL,
    english_meaning TEXT NOT NULL,
    root_arabic TEXT,
    root_english TEXT,
    quran_occurrence_count INTEGER DEFAULT 0,
    UNIQUE(surah_number, ayah_number, word_position)
);
```

**Data source:** Tanzil.net word-by-word corpus / quranenc.com — open source, free, covers all 77,430 words in the Quran.

**Seeding:** One-time Python script to parse source data and populate the table via the existing SQLite DB.

### UI Changes (AyahItem Composable)

**Default state (existing):**
- Arabic text (full ayah)
- English translation below (if toggle on)
- Action buttons: Bookmark, Highlight, Note, Tafsir, Share

**New state (word breakdown toggle on):**
- Arabic text (full ayah) — unchanged
- **Word card row** below Arabic text: horizontally scrollable row of word cards
  - Each card: Arabic word (top, large), transliteration (middle, small), English meaning (bottom, small)
  - Cards flow right-to-left to match Arabic reading direction
- Tap any word card → **Word Detail Bottom Sheet**:
  - Arabic word (large, prominent)
  - Transliteration (romanized pronunciation)
  - English meaning
  - Root word (Arabic + English)
  - "Appears X times in the Quran"
  - "Add to Word Bank" button (feeds Layer 3 flashcards)
- Toggle button in the reader top bar to show/hide word breakdown row (persisted in app_settings)

### What Doesn't Change
- Existing full-ayah translation toggle — still works independently
- Tafsir bottom sheet — unchanged
- Annotation menu (long-press) — unchanged

---

## Layer 2: "Understand This" AI Panel (Telling Mode)

### Purpose
On-demand structured AI explanation of any ayah — context, word highlights, scholar view, practical lesson.

### Trigger
New **"Understand"** button on each AyahItem action row (alongside existing Tafsir button).

### Response Structure
The AI returns a structured 4-section breakdown:

```
1. Context
   When and why this ayah was revealed (Asbab al-Nuzul).
   Historical and situational background.

2. Word Highlights
   Key Arabic words in this ayah and their specific significance.
   Why those word choices matter theologically or linguistically.

3. Scholar View
   What classical scholars say — pulled from existing tafsir DB
   (Ibn Kathir, Maarif ul Quran, Ibn Abbas). AI synthesizes.

4. Practical Lesson
   What a Muslim should take from this ayah today.
   Actionable, personal, conversational tone.
```

### Implementation

**Frontend:**
- "Understand" button on AyahItem → calls `ChatbotViewModel` with pre-built query
- Response displayed in a full bottom sheet (not the chat screen) with 4 collapsible sections
- Each section has a distinct icon and color (matching existing chatbot color scheme)
- "Open in Chat" button at bottom — lets user continue the conversation in the full chatbot

**Backend (reuses existing FastAPI RAG pipeline):**
- New endpoint: `POST /chat/understand-ayah`
- Payload: `{ surah: int, ayah: int, arabic_text: str, translation: str }`
- New system prompt focused on the 4-section teaching format (teacher persona, not reference tool)
- RAG retrieval still runs — pulls tafsir + hadith context for that ayah automatically
- Streams response back (same SSE pattern as existing `/chat/stream`)

**New system prompt (backend):**
```
You are a knowledgeable and compassionate Quran teacher. When given an ayah, 
explain it in exactly 4 sections:

CONTEXT: The historical background and reason for revelation (Asbab al-Nuzul).
WORD HIGHLIGHTS: 2-3 key Arabic words and why they specifically were chosen.
SCHOLAR VIEW: Synthesize what classical scholars say using the provided tafsir context.
PRACTICAL LESSON: One clear, personal, actionable lesson for the reader today.

Be warm, clear, and accessible. Avoid academic jargon. Cite sources like (Ibn Kathir, 2:255).
Always use the provided context — never invent scholarly opinions.
```

**New DB table:**
```sql
CREATE TABLE IF NOT EXISTS studied_ayahs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    studied_at INTEGER NOT NULL, -- Unix timestamp
    UNIQUE(surah_number, ayah_number)
);
```

---

## Layer 3: Flashcards + Progress Dashboard

### Purpose
Build long-term retention of Quranic vocabulary through spaced repetition, and give users visibility into their learning progress.

### 3a: Word Bank

Words are added to the user's personal word bank by:
- Tapping "Add to Word Bank" in the Word Detail bottom sheet (Layer 1)
- Automatically when user taps a word card (optional setting)

**New DB table:**
```sql
CREATE TABLE IF NOT EXISTS word_bank (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    word_position INTEGER NOT NULL,
    added_at INTEGER NOT NULL, -- Unix timestamp
    FOREIGN KEY(surah_number, ayah_number, word_position) 
        REFERENCES word_meanings(surah_number, ayah_number, word_position)
);
```

### 3b: Spaced Repetition (SM-2 Algorithm)

Standard SM-2 algorithm — same as Anki:
- Each word has: interval (days), ease factor (starts at 2.5), repetitions count
- After each review: user taps "Know it" / "Hard" / "Again"
  - Know it → interval multiplies by ease factor
  - Hard → interval stays, ease factor decreases
  - Again → reset to interval=1
- Words due today surface in the daily review queue

**New DB table:**
```sql
CREATE TABLE IF NOT EXISTS flashcard_reviews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word_bank_id INTEGER NOT NULL REFERENCES word_bank(id),
    next_review_at INTEGER NOT NULL, -- Unix timestamp
    interval_days INTEGER NOT NULL DEFAULT 1,
    ease_factor REAL NOT NULL DEFAULT 2.5,
    repetitions INTEGER NOT NULL DEFAULT 0,
    last_reviewed_at INTEGER,
    UNIQUE(word_bank_id)
);
```

### 3c: Flashcard UI

**Access points:**
- Daily notification: "You have X words to review" → opens practice session
- "Practice" button inside the reader (top bar) → reviews words from current surah only
- Both feed the same full-screen practice UI

**Practice session UI (full screen):**
- Card shows Arabic word (front) or English meaning (front) — alternates each session
- User thinks of answer, taps "Reveal"
- Answer shown → user taps: "Again" / "Hard" / "Know it"
- Session ends when queue is empty → summary screen (words reviewed, accuracy %)

### 3d: Progress Dashboard

**Access:** New "Progress" section inside Settings screen (not a new tab).

**Sections:**
- **Streak:** Days in a row with at least one review completed
- **Word Mastery:** X words learned / ~14,000 unique Quran words (progress bar)
- **Ayahs Studied:** X ayahs with "Understand This" used / 6,236 total (progress bar)
- **Surah Heatmap:** Grid of all 114 surahs — color intensity shows how deeply studied
  - Light = read only, Medium = word breakdown used, Dark = "Understand This" used
- **Due Today:** X words in review queue (tap to start session)

---

## Architecture Summary

### New Files (Android/KMP)

| File | Purpose |
|------|---------|
| `db/WordMeanings.sq` | word_meanings schema + queries |
| `db/WordBank.sq` | word_bank + flashcard_reviews schema |
| `db/StudiedAyahs.sq` | studied_ayahs schema |
| `ui/component/WordBreakdownRow.kt` | Horizontally scrollable word card row |
| `ui/component/WordDetailSheet.kt` | Bottom sheet for word tap detail |
| `ui/component/UnderstandSheet.kt` | 4-section AI explanation bottom sheet |
| `ui/component/FlashcardSession.kt` | Full-screen flashcard practice UI |
| `ui/component/ProgressDashboard.kt` | Progress stats and heatmap |
| `viewmodel/LearningViewModel.kt` | State for word bank, flashcards, progress |
| `repository/LearningRepository.kt` | Data access for all learning features |
| `usecase/SpacedRepetitionUseCase.kt` | SM-2 algorithm implementation |
| `usecase/UnderstandAyahUseCase.kt` | Calls backend understand-ayah endpoint |

### Modified Files

| File | Change |
|------|--------|
| `ui/component/AyahItem.kt` | Add word breakdown row + Understand button |
| `ui/screens/quran/QuranScreens.kt` | Add word breakdown toggle, Practice button |
| `ui/screens/settings/SettingsScreen.kt` | Add Progress Dashboard section |
| `viewmodel/ViewModels.kt` | Wire LearningViewModel |
| `backend/app/routers/chat.py` | Add `/chat/understand-ayah` endpoint |
| `backend/app/rag/prompt_builder.py` | Add understand-ayah system prompt |
| `data/seed_words.py` | One-time script to seed word_meanings table |

### No Changes To
- Existing chatbot, tafsir, hadith, prayer times, qibla screens
- Existing RAG pipeline logic (retriever, embedder, reranker)
- Existing DB tables (ayahs, tafsir, hadith, bookmarks, highlights, notes)
- Navigation structure (same 5 bottom tabs)

---

## Build Order

1. **Seed word data** — parse source corpus, populate `word_meanings` via migration script
2. **Layer 1** — `WordMeanings.sq` → `WordBreakdownRow` → `WordDetailSheet` → wire into `AyahItem`
3. **Layer 2** — backend `/understand-ayah` endpoint → `UnderstandSheet` → wire into `AyahItem`
4. **Layer 3a/b** — `WordBank.sq` + `FlashcardReviews.sq` → `SpacedRepetitionUseCase` → `FlashcardSession` UI
5. **Layer 3c** — `ProgressDashboard` → wire into Settings screen
6. **Notifications** — daily review reminder via Android WorkManager

---

## Success Criteria

- User can tap any Arabic word and see its meaning within 200ms (local DB, no network)
- "Understand This" returns a structured 4-section response within 5 seconds (streaming)
- Flashcard session correctly schedules next review using SM-2 after each response
- Progress dashboard accurately reflects studied ayahs and word mastery
- All existing features continue working without regression
