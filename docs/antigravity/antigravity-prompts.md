# Quran App — Antigravity Session Prompts
# Copy the relevant prompt for each phase. Paste AFTER the skills file.

---

## How to Use This File

1. Open Antigravity
2. Paste the **entire skills file** first
3. Then paste the **phase prompt** below
4. Antigravity will plan first and ask for your approval before writing any code

---

## PHASE 0 — Environment & CI Setup

```
I am building a Quran app using Compose Multiplatform (KMP). 
Please set up the complete development environment.

Tasks to complete:
1. Scaffold the KMP project using the Android Studio KMP wizard:
   - Package: com.quranapp
   - Targets: Android (primary) + iOS (stubs)
   - UI: Compose Multiplatform
   - Module name: composeApp

2. Configure composeApp/build.gradle.kts with ALL dependencies from the skills file.
   Use the libs.versions.toml pattern. Include:
   - Compose Multiplatform, Voyager, Koin, SQLDelight, Ktor, kotlinx.serialization
   - Kotlinx Coroutines, Multiplatform Settings, adhan-kotlin
   - Testing: kotlin.test, MockK, coroutines-test, JdbcSqliteDriver, compose-ui-test

3. Create the full folder structure as defined in the skills file under commonMain.

4. Create the docker-compose.yml with three services:
   - postgres (pgvector/pgvector:pg16 image)
   - fastapi (build from backend/Dockerfile)
   - nginx (nginx:alpine)
   Include proper healthchecks, volumes, and networking.

5. Create backend/Dockerfile that:
   - Uses python:3.11-slim
   - Installs sentence-transformers
   - Pre-downloads intfloat/multilingual-e5-small at build time
   - Runs uvicorn with 2 workers

6. Create nginx/nginx.conf with:
   - HTTP → HTTPS redirect
   - SSL termination
   - Proxy /api/ → fastapi:8000
   - Rate limiting on /api/chat/

7. Create .env.example with all required variables.

8. Create GitHub Actions workflows:
   - android-ci.yml: runs ./gradlew :composeApp:testDebugUnitTest
   - backend-ci.yml: runs pytest

9. Write smoke tests:
   - commonTest: SmokeTest.kt asserting 2+2==4
   - backend/tests/test_setup.py asserting 1+1==2

Before doing anything: generate a plan artifact listing all files you will create,
all dependencies you will add, and the commands you will run to verify.
Ask for my approval before writing any code.

After implementation, run:
- ./gradlew :composeApp:testDebugUnitTest (must pass)
- pytest (must pass)
- docker compose up -d (must start all 3 services)
```

---

## PHASE 1 — Data Audit

```
I need to audit all my Islamic data JSON files before building the app database.

My data is located at:
- data/ayahs/ — Quran Ayahs with Arabic (Hafs + Warsh) and English translation
- data/tafsir/2-TafsirIbnAbbasInEnglish/ — Ibn Abbas Tafsir
- data/tafsir/3-Maarif Ul Quran English/ — Maarif ul Quran
- data/tafsir/4-Ibn Kathir English/ — Ibn Kathir
- data/hadith/Sahih_Bukhari/
- data/hadith/Sahih_Muslim/
- data/hadith/Sunan_Abu_Dawood/
- data/hadith/Jami_at_Tirmidhi/
- data/hadith/Sunan_an_Nasai/
- data/hadith/Sunan_Ibn_Majah/

Tasks:
1. Write scripts/audit_jsons.py that checks:
   - Ayahs: exactly 6236 records, all 114 Surahs present, no empty Arabic/translation
   - Each Tafsir book: file exists, entries > 0, covers >= 6000 Ayahs, has surah+ayah refs
   - Each Hadith collection: folder exists, files not empty, has hadith_number + translation

2. Write backend/tests/test_data_quality.py with pytest parametrized tests:
   - TestAyahs (4 tests)
   - TestTafsir parametrized over 3 books (3 books × 3 tests = 9 tests)
   - TestHadith parametrized over 6 collections (6 × 3 tests = 18 tests)
   Total: 31 tests

3. Run the audit script and report what it finds.

4. Write scripts/build_sqlite.py that:
   - Reads all validated JSON files
   - Applies scripts/schema.sql
   - Populates all tables (Surahs, quran_pages, Ayahs with Hafs+Warsh,
     Tafsir for 3 books, Hadith for 6 collections)
   - Reports final DB size
   - Outputs to composeApp/src/androidMain/assets/quran.db

Plan first. Ask for approval. Then implement.
All 31 pytest tests must pass before this phase is complete.
```

---

## PHASE 2 — SQLDelight Schema & Offline DB

```
Set up the complete SQLDelight offline database layer for the Quran app.

Tables required:
1. surahs — metadata (number, name Arabic/English/transliteration, revelation type, ayah count)
2. quran_pages — page-to-Ayah mapping (604 Mushaf pages)
3. ayahs — id, surah_number, ayah_number, page_number, juz_number,
           arabic_text_hafs, arabic_text_warsh, translation_english
4. tafsir — surah_number, ayah_number, book_name (ibn_kathir|maarif|ibn_abbas), content
5. hadith — collection, book_number, hadith_number, arabic_text, translation, narrator
6. bookmarks — type (ayah|hadith), reference_id, created_at
7. highlights — ayah_id, color, created_at
8. notes — type, reference_id, content, updated_at
9. reading_position — single row: surah_number, ayah_number, page_number, mode
10. app_settings — key/value (quran_script, theme, reading_mode)

For each table, write named SQLDelight queries covering:
- SELECT operations needed by the UI
- INSERT/UPDATE/DELETE for user data tables
- Search queries for ayahs and hadith (LIKE on translation)
- Page-based and surah-based Ayah queries

Then write commonTest tests using JdbcSqliteDriver(IN_MEMORY):
- QuranLocalDataSourceTest (6+ tests)
- HadithLocalDataSourceTest (4+ tests)
- UserDataLocalDataSourceTest (12+ tests covering bookmarks, highlights, notes)
- ReadingPositionTest (3 tests)
- AppSettingsTest (3 tests)

Also create the DatabaseDriverFactory expect/actual:
- commonMain: expect class
- androidMain: actual using AndroidSqliteDriver, copies quran.db from assets on first launch
- iosMain: actual using NativeSqliteDriver (stub for MVP)

Plan first. Ask for approval. All tests must pass before completion.
Run: ./gradlew :composeApp:testDebugUnitTest
```

---

## PHASE 3 — Domain Layer & Use Cases

```
Build the complete domain layer for the Quran app. Zero Android imports allowed in commonMain.

1. Create all domain models in domain/model/:
   - Ayah (with fun arabicText(script: QuranScript) helper)
   - Surah, TafsirEntry, Hadith
   - Bookmark, Highlight, Note
   - PrayerTimesResult, NextPrayer
   - ChatMessage, ChatRole, ChatSources, AyahReference, HadithReference
   - SearchResult (sealed class: AyahResult, HadithResult)
   - QuranScript enum (HAFS, WARSH)
   - ReadingMode enum (PAGE, SCROLL)

2. Create repository interfaces in domain/repository/ (interfaces only, no implementations)

3. Create all use cases in domain/usecase/:
   - GetSurahListUseCase
   - GetAyahsForPageUseCase (validates 1..604)
   - GetAyahsBySurahUseCase (validates 1..114)
   - GetTafsirUseCase
   - GetCollectionsUseCase
   - GetHadithByBookUseCase
   - SearchUseCase (min 3 chars, runs Ayah + Hadith search in parallel with coroutineScope)
   - ToggleBookmarkUseCase
   - SetHighlightUseCase
   - SaveNoteUseCase
   - GetPrayerTimesUseCase
   - GetQiblaDirectionUseCase
   - SendChatMessageUseCase

4. Create ViewModels (ScreenModel) for:
   - QuranViewModel (loadSurah, loadPage, toggleTranslation, toggleScript, toggleMode)
   - HadithViewModel (loadCollections, loadBook)
   - SearchViewModel (search, clearResults)
   - ChatbotViewModel (sendMessage, clearHistory)
   - PrayerTimesViewModel (loadForLocation)
   - QiblaViewModel (updateBearing)
   - SettingsViewModel (updateTheme, updateScript, updateReadingMode)

5. Write commonTest tests for ALL use cases and ViewModels.
   Minimum: 4 tests per use case, 4 tests per ViewModel.
   Use MockK for all repository mocking.
   Use runTest + StandardTestDispatcher for ViewModel tests.

Plan first. Ask for approval.
Run: ./gradlew :composeApp:testDebugUnitTest
All tests must pass.
```

---

## PHASE 4 — Quran Reader UI

```
Build the complete Quran reader UI. This is the most important screen in the app.

Requirements:

1. QuranHomeScreen (Surah list):
   - LazyColumn of 114 Surahs
   - Each row: surah number badge, Arabic name, English name, revelation type, ayah count
   - Tap → navigates to QuranReaderScreen

2. QuranReaderScreen with two modes toggled by user:

   SCROLL MODE (default):
   - LazyColumn of AyahItem composables
   - Sticky header per Surah showing Surah name + Basmallah
   - Each AyahItem:
     - Arabic text (selected script), RTL, Uthmani font, min 22sp, full width
     - Ayah number badge (right side, Arabic numerals)
     - Translation text (English) below Arabic — toggleable
     - "Tafsir" button
     - Long press → AnnotationMenu bottom sheet

   PAGE MODE (Mushaf layout):
   - HorizontalPager with 604 pages
   - Each page: flowing RTL Arabic text filling the page
   - Page number at bottom center
   - Juz/Hizb marker at top
   - Same long press behavior

3. Top bar:
   - Surah name (current)
   - Translation toggle icon button
   - Layout toggle icon (page/scroll)
   - Script toggle (Hafs/Warsh)

4. TafsirBottomSheet:
   - TabRow with 3 tabs: "Ibn Kathir" | "Maarif" | "Ibn Abbas"
   - HorizontalPager matching tabs
   - Each tab: scrollable tafsir text from SQLDelight
   - Loading state while fetching

5. AnnotationMenu (ModalBottomSheet on long press):
   - "Bookmark" toggle button (filled/outlined based on state)
   - Color picker row (5 colors: yellow, green, blue, pink, clear)
   - "Add Note" button → opens text input dialog

6. Write androidTest Compose UI tests:
   - quranReader_showsArabicText
   - quranReader_translationToggle_hidesTranslation
   - quranReader_switchToPageMode_showsMushafLayout
   - quranReader_longPress_showsAnnotationMenu
   - tafsirSheet_showsThreeTabs
   - tafsirSheet_ibmKathirTabLoadsContent
   - arabicText_isRightAligned

Arabic text rules:
- textAlign = TextAlign.Right
- LayoutDirection is forced RTL for Arabic text blocks
- Font: Uthmani (load from composeResources/fonts/)
- Do NOT use system default for Arabic rendering

Plan first. Ask for my approval. 
Write UI tests first, then implement components.
Test on a real Android device — RTL must render correctly.
```

---

## PHASE 5 — Hadith Browser UI

```
Build the complete Hadith browser UI with 3 screens.

1. HadithCollectionsScreen:
   - LazyColumn of 6 collection cards
   - Each card: full name, hadith count, book count
   - Collection names: Sahih Bukhari, Sahih Muslim, Sunan Abu Dawood,
     Jami at-Tirmidhi, Sunan an-Nasai, Sunan Ibn Majah
   - Tap → HadithBooksScreen

2. HadithBooksScreen:
   - Collection name in TopBar
   - LazyColumn of book rows (number + hadith count)
   - Tap → HadithListScreen

3. HadithListScreen:
   - Book title + collection in TopBar
   - Search bar at top (filters displayed list)
   - LazyColumn of HadithCard
   - HadithCard contains:
     - Reference badge: "[Collection] Book X, Hadith Y"
     - Narrator name (italic)
     - Translation text
     - Arabic text (hidden by default, toggle button)
     - Long press → AnnotationMenu (same as Quran reader)
   - Smooth scrolling even for large books (Bukhari ~7500 hadiths)

4. Write androidTest Compose UI tests:
   - collections_showsAllSixCollections
   - collections_tapBukhari_navigatesToBooks
   - hadithCard_showsTranslation
   - hadithCard_arabicToggle_showsArabicText
   - hadithCard_longPress_showsAnnotationMenu
   - hadithList_search_filtersResults

Plan first. Ask for approval.
All tests must pass before completion.
```

---

## PHASE 6 — Bookmarks, Highlights & Notes

```
Wire up the complete annotation system end-to-end from UI to SQLDelight.

The AnnotationMenu bottom sheet (built in Phase 4) should now fully work:

1. Bookmark toggle:
   - Reads current state from UserDataViewModel.isBookmarked()
   - Tap → calls ToggleBookmarkUseCase → updates SQLDelight → updates UI immediately
   - Bookmark icon on AyahItem/HadithCard updates reactively via StateFlow

2. Highlight color picker:
   - 5 color options: #FFD700 (yellow), #90EE90 (green), #87CEEB (blue), #FFB6C1 (pink), clear
   - Tap color → calls SetHighlightUseCase → AyahItem background updates immediately
   - "Clear" option removes highlight

3. Notes:
   - "Add Note" → shows AlertDialog with TextField
   - Save → calls SaveNoteUseCase → note icon appears on AyahItem
   - Tap note icon → opens dialog with existing note pre-filled for editing

4. Bookmarks list screen (accessible from settings/profile tab):
   - Shows all bookmarked Ayahs and Hadiths
   - Tap → navigates to that Ayah/Hadith

5. All state must persist after app restart — stored in SQLDelight.

Write commonTest ViewModel tests:
- toggleBookmark_addsWhenNotBookmarked
- toggleBookmark_removesWhenAlreadyBookmarked
- setHighlight_updatesState
- setHighlight_clear_removesHighlight
- saveNote_updatesState
- saveNote_existingNote_updatesContent

Write androidTest UI tests:
- annotationMenu_bookmarkToggle_updatesIcon
- annotationMenu_highlightColor_updatesBackground
- annotationMenu_saveNote_showsNoteIcon

Plan first. Ask for approval. All tests must pass.
```

---

## PHASE 7 — Search

```
Build the unified offline search screen that searches Quran and Hadith simultaneously.

1. SearchScreen:
   - SearchBar at top (Material 3 SearchBar component)
   - Minimum 3 characters before search fires
   - Results appear below in LazyColumn
   - Results are mixed: AyahResult and HadithResult items shown together
   - Each AyahResult card: reference (Surah:Ayah), matched translation text with highlighted query term
   - Each HadithResult card: collection + number, matched translation with highlighted query term
   - Tap any result → navigates to that Ayah in QuranReaderScreen or Hadith in HadithListScreen
   - Empty state: "No results found for X"
   - Loading state while query runs

2. SearchUseCase runs Ayah and Hadith queries in parallel using coroutineScope + async.

3. Highlight matched query terms in result text using SpanStyle in AnnotatedString.

Write commonTest tests:
- searchUseCase_emptyQuery_returnsFailure
- searchUseCase_queryUnder3Chars_returnsFailure
- searchUseCase_validQuery_returnsCombinedResults
- searchUseCase_queriesRunInParallel
- searchViewModel_updatesResultsOnQuery
- searchViewModel_clearsResultsOnClear

Write androidTest UI tests:
- searchScreen_under3Chars_doesNotSearch
- searchScreen_validQuery_showsResults
- searchScreen_noResults_showsEmptyState
- searchScreen_tapResult_navigates

Plan first. Ask for approval. All tests must pass.
```

---

## PHASE 8 — Prayer Times, Notifications & Qibla

```
Build prayer times calculation, custom Azan alarms, and Qibla compass.

1. Prayer Times Screen:
   - Requests GPS location permission on first open
   - If denied: shows manual city/country text input as fallback
   - Shows all 5 prayer times for today (Fajr, Dhuhr, Asr, Maghrib, Isha)
   - Highlights the next prayer with countdown timer
   - Each prayer row has on/off toggle for notification
   - "Calculation Method" selector: Muslim World League (default), ISNA, Egypt, etc.

2. Prayer Notifications (androidMain actual implementation):
   - Uses AlarmManager.setExactAndAllowWhileIdle() for reliability
   - Custom sound: user can pick a sound file or use default system Azan
   - PrayerAlarmReceiver (BroadcastReceiver) shows notification + plays sound
   - Foreground service for audio playback
   - Alarms reschedule daily automatically
   - Handles BOOT_COMPLETED to reschedule after device restart

3. Qibla Screen:
   - Compass needle points toward Mecca
   - Rotates in real-time using CompassSensor (expect/actual with SensorManager on Android)
   - Shows numeric degree and cardinal direction
   - Calibration hint if sensor accuracy is low
   - Works fully offline

4. iOS stubs (iosMain):
   - LocationProvider returns null
   - CompassSensor emits 0f constant
   - NotificationScheduler is a no-op
   All must compile without errors.

Write commonTest tests (no device needed):
- getPrayerTimesUseCase_returnsAllFivePrayers
- getPrayerTimesUseCase_timesAreChronological
- getPrayerTimesUseCase_differentLocationsGiveDifferentTimes
- getQiblaUseCase_londonIs119Degrees (±1)
- getQiblaUseCase_newYorkIs59Degrees (±1)
- getQiblaUseCase_bearingIsBetween0And360

Plan first. Ask for approval.
Test prayer alarm on a real device with 1 minute offset before marking complete.
```

---

## PHASE 9 — RAG Backend

```
Build the complete custom RAG backend pipeline and deploy to Azure VPS.

1. PostgreSQL schema (backend/init.sql — runs on container startup):
   - CREATE EXTENSION IF NOT EXISTS vector
   - ayah_embeddings (id, surah_number, ayah_number, content, embedding vector(384))
   - hadith_embeddings (id, collection, hadith_number, content, embedding vector(384))
   - tafsir_embeddings (id, surah_number, ayah_number, book_name, content, embedding vector(384))
   - HNSW indexes on all 3 embedding columns

2. backend/app/rag/embedder.py:
   - Singleton model loading with lru_cache
   - embed_query(text): prefixes "query: ", returns normalized 384-dim list
   - embed_passage(text): prefixes "passage: ", returns normalized 384-dim list

3. backend/app/rag/retriever.py:
   - retrieve(query, db): runs 3 parallel pgvector similarity queries
   - Filters similarity > 0.30
   - Returns dict: {ayahs, hadiths, tafsir}

4. backend/app/rag/prompt_builder.py:
   - SYSTEM_PROMPT: citation rules, no fabrication, scholar disclaimer
   - build_prompt(query, retrieved): formats chunks with references

5. backend/app/rag/llm_client.py:
   - Anthropic SDK wrapper
   - Model: claude-sonnet-4-20250514
   - temperature=0.2, max_tokens=1000

6. backend/app/routers/chat.py:
   - POST /chat/ with request validation (not empty, max 1000 chars)
   - GET /health returning status + model_loaded + db_connected

7. Ingestion scripts (run once after deployment):
   - ingestion/ingest_ayahs.py
   - ingestion/ingest_tafsir.py (3 books: ibn_kathir, maarif, ibn_abbas)
   - ingestion/ingest_hadith.py (6 collections)

Write pytest TDD tests:
- test_embedder.py: dimension, normalization, Arabic text, similarity
- test_retriever.py: correct keys, 3 DB queries, returns lists
- test_prompt_builder.py: cites ayah, cites hadith, handles empty, has disclaimer
- test_chat_router.py: 200 valid, 400 empty, 400 too long, health endpoint

After tests pass:
- Build Docker image: docker compose build fastapi
- Start all services: docker compose up -d
- Run ingestion scripts
- Verify: curl https://your-domain.com/api/health
- Manual test: send 5 Islamic questions, verify relevant citations returned

Plan first. Ask for approval. All 16 backend tests must pass.
```

---

## PHASE 10 — AI Chatbot UI

```
Build the AI chatbot screen connected to the FastAPI RAG backend.

1. ChatbotScreen:
   - Full-screen chat interface
   - LazyColumn of ChatMessage items (reversed, newest at bottom)
   - User messages: right-aligned, primary color background
   - Assistant messages: left-aligned, surface color background
   - Loading indicator (animated dots) while waiting for response
   - Text input + send button at bottom
   - OfflineBanner at top when no internet connection

2. ChatBubble component:
   - Renders markdown-style text (bold for citations)
   - Assistant bubbles include SourceCard list below the text
   - Scholar disclaimer always shown below assistant responses

3. SourceCard component:
   - Tappable card showing "Surah 2:153" or "Bukhari #1469" or "Ibn Kathir on 2:153"
   - Tap → navigates to that Ayah in QuranReaderScreen or Hadith in HadithListScreen
   - Different icon for Ayah vs Hadith vs Tafsir sources

4. Ktor HTTP client setup:
   - Base URL from BuildConfig (points to Azure VPS HTTPS endpoint)
   - Timeout: 30 seconds
   - JSON content negotiation
   - Error handling: network errors → show "Check your internet connection"
   - 503 errors → show "Service temporarily unavailable"

5. Offline detection:
   - Use ConnectivityManager (androidMain actual) to detect network state
   - Show OfflineBanner when offline
   - Disable send button when offline

Write commonTest ViewModel tests:
- initial_stateIsEmpty
- sendMessage_userMessageAppearsImmediately
- sendMessage_assistantResponseOnSuccess
- sendMessage_errorMessageOnFailure
- sendMessage_emptyMessageIsIgnored

Write androidTest UI tests:
- chatScreen_showsOfflineBanner_whenOffline
- chatBubble_showsScholarDisclaimer
- sourceCard_showsAyahReference
- sourceCard_tap_navigatesToAyah

Plan first. Ask for approval. All tests must pass.
Verify end-to-end on real device with real VPS connection.
```

---

## PHASE 11 — Theme, Polish & Play Store

```
Final phase: dark/light theme, performance polish, Play Store submission.

1. Complete Theme.kt with Material 3:
   Dark theme:
   - primary: #4CAF7D (emerald green)
   - background: #0D1117 (deep dark)
   - surface: #161B22
   - onBackground: #E6EDF3
   
   Light theme (Mushaf parchment style):
   - primary: #2D6A4F (deep forest green)
   - background: #F5F0E8 (cream/parchment)
   - surface: #FFFFFF
   - onBackground: #1A1A1A

2. Theme toggle in SettingsScreen:
   - Toggle switch: Light / Dark
   - Persisted in SQLDelight app_settings table with key "theme"
   - Applied immediately on toggle using rememberUpdatedState

3. Performance audit — fix anything failing these targets:
   - App cold start < 3 seconds (measure with Macrobenchmark)
   - Surah list first frame < 200ms
   - Mushaf page swipe: 60fps (no frame drops in systrace)
   - SQLDelight queries < 50ms (log slow queries)
   - Search results < 300ms
   - Chatbot response < 8 seconds end-to-end

4. Final integration checklist (test on real Android device):
   - First launch: DB copy completes, Surah list appears
   - Page-by-page mode: swipe works, 604 pages accessible
   - Scroll mode: continuous Ayah list
   - Script toggle: Hafs → Warsh updates all text immediately
   - Translation toggle: works in both modes
   - Tafsir: all 3 books load content
   - Hadith: all 6 collections accessible offline
   - Bookmarks persist after app restart
   - Highlights persist after app restart
   - Notes save and reload
   - Prayer times accurate for GPS location
   - Prayer alarm fires at correct time
   - Qibla compass rotates correctly
   - Search finds Quran + Hadith results
   - Chatbot: sends question, receives answer with citations
   - Citations tap → navigate to correct Ayah/Hadith
   - Offline: all features work except chatbot
   - Chatbot offline: shows offline banner, send disabled

5. Play Store assets to create:
   - App icon 512×512 PNG (no transparency, Islamic geometric design)
   - Feature graphic 1024×500 PNG
   - 4 phone screenshots: Quran reader page mode, Hadith browser, Chatbot, Prayer times
   - Short description (80 chars): "Complete Quran, Hadith & Islamic AI assistant. Fully offline."
   - Privacy policy (needed for GPS usage)

6. Release build:
   ./gradlew :composeApp:bundleRelease
   Sign with upload keystore.
   Upload to Play Store internal testing track first.

Plan first. Ask for approval.
App must pass full integration checklist before Play Store submission.
```

---

## General Task Prompt Template
Use this for any ad-hoc task within a phase:

```
[Describe what you want to build or fix]

Context:
- Current phase: Phase X
- File(s) involved: [list files]
- Related existing code: [describe what's already there]

Requirements:
- [bullet list of specific requirements]

TDD requirement:
- Write the test first in [commonTest / androidTest / backend/tests]
- Show me the failing test before implementing

Plan first, ask for approval, then implement.
Run tests and confirm they pass before finishing.
```
