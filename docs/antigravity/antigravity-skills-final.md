# Quran App — Antigravity Skills File
# Paste this at the START of every Antigravity session, before any task.

---

## Project Identity
- **App name:** Quran App
- **Package:** `com.quranapp`
- **Module:** `composeApp` (single KMP module)
- **Platform:** Android-first. iOS stubs compiled but not functional in MVP.
- **IDE:** Antigravity
- **Methodology:** TDD — Red → Green → Refactor. No production code without a failing test first.

---

## Current MVP Phase Status
Update this as phases complete — paste updated version at start of each session.

- [ ] Phase 0  — Environment & CI Setup
- [ ] Phase 1  — Data Audit
- [ ] Phase 2  — SQLDelight Schema & Offline DB
- [ ] Phase 3  — Domain Layer & Use Cases
- [ ] Phase 4  — Quran Reader UI
- [ ] Phase 5  — Hadith Browser UI
- [ ] Phase 6  — Bookmarks, Highlights & Notes
- [ ] Phase 7  — Search
- [ ] Phase 8  — Prayer Times, Notifications & Qibla
- [ ] Phase 9  — RAG Backend
- [ ] Phase 10 — AI Chatbot UI
- [ ] Phase 11 — Theme, Polish & Play Store

---

## Stack — Never Deviate

### Mobile
| Concern | Library | Version |
|---|---|---|
| UI | Compose Multiplatform | 1.6.x |
| Language | Kotlin K2 | 2.0.x |
| Navigation | Voyager | 1.0.x |
| DI | Koin | 3.5.x |
| On-device DB | SQLDelight | 2.x |
| HTTP | Ktor Client | 2.x |
| JSON | kotlinx.serialization | 1.6.x |
| Async | Kotlinx Coroutines | 1.8.x |
| Settings | Multiplatform Settings | 1.1.x |
| Prayer times | adhan-kotlin | 1.2.x |
| Testing (logic) | kotlin.test + MockK + coroutines-test | commonTest |
| Testing (UI) | compose-ui-test | androidTest |

### Backend
| Concern | Library |
|---|---|
| Framework | FastAPI 0.111+ (Pydantic v2) |
| Embedding | sentence-transformers `intfloat/multilingual-e5-small` |
| DB | PostgreSQL 16 + pgvector (Docker, self-hosted Azure VPS) |
| ORM | SQLAlchemy 2.x + asyncpg |
| LLM | Anthropic SDK `claude-sonnet-4-20250514` |
| Server | Uvicorn |
| Testing | pytest + pytest-asyncio + httpx |

### Infrastructure
| Service | Purpose |
|---|---|
| Azure VPS (B2s min — 2 vCPU, 4GB RAM) | Hosts all Docker containers |
| Docker Compose | FastAPI + PostgreSQL + Nginx |
| Nginx | Reverse proxy + SSL (Let's Encrypt) |
| GitHub Actions | CI/CD |

### BANNED libraries — never suggest these:
- ❌ Hilt (Android-only — use Koin)
- ❌ Room (Android-only — use SQLDelight)
- ❌ Retrofit (use Ktor)
- ❌ Gson (use kotlinx.serialization)
- ❌ LiveData (use StateFlow)
- ❌ RxJava (use Coroutines)
- ❌ Navigation-Compose Jetpack (use Voyager)
- ❌ LangChain / LlamaIndex (custom RAG only)
- ❌ Any managed vector DB (Pinecone, Weaviate — use self-hosted pgvector)

---

## Architecture — Strictly Clean Architecture

```
commonMain/kotlin/com/quranapp/
├── ui/           ← Composables only. No business logic.
├── viewmodel/    ← ScreenModel. StateFlow. screenModelScope only.
├── domain/
│   ├── model/    ← Pure Kotlin data classes. Zero framework imports.
│   ├── repository/ ← Interfaces only.
│   └── usecase/  ← suspend operator fun invoke(). Returns Result<T>.
├── data/
│   ├── local/    ← SQLDelight wrappers.
│   ├── remote/   ← Ktor client (chatbot only).
│   └── repository/ ← Repository implementations.
└── di/           ← Koin modules only.
```

**Import rules:**
- `ui` → imports `viewmodel` only
- `viewmodel` → imports `domain` only
- `domain` → imports Kotlin stdlib only
- `data` → implements `domain` interfaces, imports SQLDelight + Ktor

---

## TDD Rules — Non-Negotiable

1. **Write the test first.** Watch it fail. Then write code to pass it.
2. **Test location:**
   - Logic (ViewModel, UseCase, Repository, DataSource) → `commonTest`
   - Composable rendering → `androidTest`
   - Backend → `backend/tests/` (pytest)
3. **Run after every change:**
   - `./gradlew :composeApp:testDebugUnitTest` (commonTest)
   - `./gradlew :composeApp:connectedAndroidTest` (UI tests)
   - `pytest` (backend)
4. **Never skip a test.** Not testable = architecture problem.
5. **Use MockK** for all mocking in Kotlin tests.

---

## SQLDelight Rules

- Schema in `.sq` files: `commonMain/sqldelight/com/quranapp/db/`
- One `.sq` file per table
- Use `JdbcSqliteDriver(IN_MEMORY)` in `commonTest`
- Use `AndroidSqliteDriver` in `androidMain`
- Use `NativeSqliteDriver` in `iosMain`
- **Never write raw SQL strings in Kotlin.** Always use generated SQLDelight functions.

---

## ViewModel (ScreenModel) Pattern

```kotlin
class ExampleViewModel(
    private val useCase: ExampleUseCase
) : ScreenModel {                           // NOT Android ViewModel

    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    fun doSomething() {
        screenModelScope.launch {           // NOT viewModelScope
            _uiState.update { it.copy(isLoading = true, error = null) }
            useCase()
                .onSuccess { data ->
                    _uiState.update { it.copy(data = data, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }
}

data class ExampleUiState(
    val data: List<Something> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## Use Case Pattern

```kotlin
class GetAyahsBySurahUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(surahNumber: Int): Result<List<Ayah>> {
        if (surahNumber !in 1..114)
            return Result.failure(IllegalArgumentException("Surah must be 1–114"))
        return runCatching { repository.getAyahsBySurah(surahNumber) }
    }
}
```

Rules:
- Always `suspend operator fun invoke`
- Always returns `Result<T>`
- Always validates input before calling repository
- Uses `runCatching` — never catches silently

---

## expect/actual — Current List

| Feature | androidMain (real) | iosMain (stub — MVP) |
|---|---|---|
| `DatabaseDriverFactory` | `AndroidSqliteDriver` | `NativeSqliteDriver` |
| `LocationProvider` | `FusedLocationProviderClient` | Returns `null` |
| `CompassSensor` | `SensorManager` | Returns `Flow { emit(0f) }` |
| `NotificationScheduler` | `AlarmManager` + `NotificationManager` | No-op |

iOS stubs must compile and return safe defaults — no crashes, no `TODO()` throws.

---

## Quran App Specific Rules

### Quran Reading
- **Two modes:** PAGE (Mushaf page-by-page, 604 pages) and SCROLL (continuous by Surah)
- **Two scripts:** HAFS and WARSH — user selects in settings, stored in `app_settings` table
- Arabic text: `fontFamily = uthamanicFontFamily`, size 22sp minimum, `textAlign = Right`, `writingDirection = Rtl`
- Page-by-page uses `HorizontalPager` — swipe left/right
- Scroll uses `LazyColumn` with sticky Surah headers
- Reading position saved to SQLDelight on every Ayah/page change

### Tafsir
- Three books: `ibn_kathir`, `maarif`, `ibn_abbas`
- Shown in `BottomSheet` with `TabRow` — one tab per book
- Keyed by `(surah_number, ayah_number, book_name)` in SQLDelight

### Prayer Notifications
- Each of 5 prayers has independent on/off toggle
- Custom sound URI per prayer (or default system Azan)
- Uses `AlarmManager.setExactAndAllowWhileIdle()` for reliability
- Reschedule alarms daily after each fires
- `PrayerAlarmReceiver` (BroadcastReceiver) handles the alarm

### Chatbot
- Online only — show `OfflineBanner` when no internet
- Every assistant message ends with scholar disclaimer
- Source cards are tappable — tap navigates to Ayah or Hadith
- Temperature 0.2, max_tokens 1000
- Never fabricate — if no relevant chunk found, say so

### Theme
- Dark: deep green primary `#4CAF7D`, dark background `#0D1117` (GitHub dark style)
- Light: deep green primary `#2D6A4F`, cream background `#F5F0E8` (Mushaf parchment style)
- User toggle in settings, persisted in `app_settings` table with key `theme`

---

## Antigravity Session Protocol

When I give you a task, always:

1. **Plan first** — generate artifact showing:
   - Files to create/modify
   - New dependencies (if any)
   - Test file to write first
   - Commands to verify
2. **Ask for approval** before writing any code
3. **Write test first** — run → confirm FAIL (Red)
4. **Implement** minimum code → run → confirm PASS (Green)
5. **Refactor** → run → confirm still PASS
6. **Report** — list completed items and passing test count

---

## File Locations Quick Reference

| What | Where |
|---|---|
| SQLDelight schemas (.sq files) | `composeApp/src/commonMain/sqldelight/com/quranapp/db/` |
| Bundled SQLite DB | `composeApp/src/androidMain/assets/quran.db` |
| Koin DI modules | `composeApp/src/commonMain/kotlin/com/quranapp/di/` |
| Common unit tests | `composeApp/src/commonTest/kotlin/com/quranapp/` |
| Android UI tests | `composeApp/src/androidTest/kotlin/com/quranapp/` |
| Android actual impls | `composeApp/src/androidMain/kotlin/com/quranapp/actual/` |
| iOS stubs | `composeApp/src/iosMain/kotlin/com/quranapp/actual/` |
| FastAPI app | `backend/app/` |
| FastAPI tests | `backend/tests/` |
| Ingestion scripts | `backend/ingestion/` |
| Raw JSON data | `data/ayahs/`, `data/tafsir/`, `data/hadith/` |
| Utility scripts | `scripts/` |
| Docker config | `docker-compose.yml`, `nginx/nginx.conf`, `backend/Dockerfile` |

---

## Gradle Commands

```bash
# Common unit tests (no emulator needed — run always)
./gradlew :composeApp:testDebugUnitTest

# Android UI tests (emulator/device required)
./gradlew :composeApp:connectedAndroidTest

# Install on connected Android device
./gradlew :composeApp:installDebug

# Build release AAB
./gradlew :composeApp:bundleRelease

# Backend tests
cd backend && pytest --cov=app -v

# Build quran.db from JSON files
python scripts/build_sqlite.py

# Audit JSON data quality (run before Phase 2)
python scripts/audit_jsons.py

# Docker — local dev
docker compose up -d
docker compose logs -f fastapi
docker compose exec fastapi pytest

# Run ingestion
docker compose exec fastapi python ingestion/ingest_ayahs.py
docker compose exec fastapi python ingestion/ingest_tafsir.py
docker compose exec fastapi python ingestion/ingest_hadith.py
```

---

## Hard Rules — Never Violate

1. No `GlobalScope` — use `screenModelScope` in ScreenModel, `coroutineScope` in use cases
2. No `!!` non-null assertion — use safe calls or `requireNotNull("message")`
3. No Android-specific imports in `commonMain`
4. No business logic in Composables — state lives in ViewModel only
5. No `Thread.sleep()` in tests — use `advanceUntilIdle()` or `runTest { delay() }`
6. No hardcoded strings in Composables — use string resources
7. No feature without a test
8. No `@Suppress` without a comment explaining why
9. No direct SQL strings in Kotlin — use SQLDelight generated APIs only
10. No commits with failing tests

---
*Skills File v1.0 — Update Phase Status section at start of each session*
