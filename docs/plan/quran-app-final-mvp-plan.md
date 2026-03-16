# 📖 Quran App — Final MVP Plan
> **Mobile:** Compose Multiplatform (CMP) — Android-first, iOS post-MVP
> **Backend:** FastAPI + Custom RAG Pipeline
> **Database:** PostgreSQL + pgvector (single self-hosted instance)
> **Deployment:** Azure VPS + Docker Compose + Nginx
> **IDE:** Antigravity
> **Methodology:** TDD — Red → Green → Refactor, every phase

---

## ✅ MVP Feature Scope

| Feature | Detail |
|---|---|
| Quran Reader | Mushaf-style Arabic layout — page-by-page AND continuous scroll (user switches) |
| Arabic Script | Hafs + Warsh — user selects in settings |
| Translation | English sentence translation, toggle on/off per Ayah |
| Tafsir Panel | Ibn Kathir, Maarif ul Quran, Ibn Abbas — 3-tab bottom sheet per Ayah |
| Hadith Browser | Bukhari, Muslim, Abu Dawood, Tirmidhi, Nasai, Ibn Majah — offline |
| AI Chatbot | Custom RAG — retrieves Ayahs + Hadith + Tafsir, tappable citations |
| Bookmarks | Ayahs + Hadith — SQLDelight local storage |
| Highlights | Color highlights on Ayahs — SQLDelight local storage |
| Notes | Personal notes per Ayah or Hadith — SQLDelight local storage |
| Search | Unified offline search across Quran + Hadith |
| Prayer Times | Offline calculation via adhan-kotlin, GPS-based |
| Prayer Notifications | Custom Azan alarm per prayer, platform foreground service |
| Qibla Compass | Offline, device magnetometer |
| Theme | Dark + Light mode, user toggles, persisted in settings |
| UI Language | English only |

## ❌ Post-MVP (Deliberately Excluded)
- Word-by-word translation
- Firebase Auth / user accounts / cloud sync
- Audio recitation
- Arabic root explorer
- iOS `actual` implementations (stubs compiled, not functional)
- Urdu / Arabic UI language
- Multiple translation options

---

## Table of Contents
1. [Architecture](#1-architecture)
2. [Complete Tech Stack](#2-complete-tech-stack)
3. [Infrastructure & Deployment](#3-infrastructure--deployment)
4. [Project Structure](#4-project-structure)
5. [TDD Rules](#5-tdd-rules)
6. [Phase 0 — Environment & CI Setup](#6-phase-0--environment--ci-setup)
7. [Phase 1 — Data Audit](#7-phase-1--data-audit)
8. [Phase 2 — SQLDelight Schema & Offline DB](#8-phase-2--sqldelight-schema--offline-db)
9. [Phase 3 — Domain Layer & Use Cases](#9-phase-3--domain-layer--use-cases)
10. [Phase 4 — Quran Reader UI](#10-phase-4--quran-reader-ui)
11. [Phase 5 — Hadith Browser UI](#11-phase-5--hadith-browser-ui)
12. [Phase 6 — Bookmarks, Highlights & Notes](#12-phase-6--bookmarks-highlights--notes)
13. [Phase 7 — Search](#13-phase-7--search)
14. [Phase 8 — Prayer Times, Notifications & Qibla](#14-phase-8--prayer-times-notifications--qibla)
15. [Phase 9 — RAG Backend](#15-phase-9--rag-backend)
16. [Phase 10 — AI Chatbot UI](#16-phase-10--ai-chatbot-ui)
17. [Phase 11 — Theme, Polish & Play Store](#17-phase-11--theme-polish--play-store)
18. [Database Schemas](#18-database-schemas)
19. [Docker & Deployment](#19-docker--deployment)
20. [API Contract](#20-api-contract)
21. [RAG Pipeline Design](#21-rag-pipeline-design)
22. [Testing Strategy](#22-testing-strategy)
23. [Risk Register](#23-risk-register)
24. [MVP → Post-MVP Checklist](#24-mvp--post-mvp-checklist)

---

## 1. Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                  COMPOSE MULTIPLATFORM APP                       │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌────────┐  │
│  │  Quran   │ │  Hadith  │ │  Search  │ │Chatbot │ │Prayer/ │  │
│  │  Reader  │ │  Browser │ │          │ │        │ │ Qibla  │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └───┬────┘ └───┬────┘  │
│       │             │            │            │          │       │
│  ┌────▼─────────────▼────────────▼────────────▼──────────▼────┐ │
│  │              ViewModel Layer (commonMain)                   │ │
│  │              Koin DI + Kotlin Coroutines + StateFlow        │ │
│  └────┬────────────────────────────────────────────────────────┘ │
│       │                                                          │
│  ┌────▼──────────────────────────────────┐  ┌─────────────────┐ │
│  │     SQLDelight (OFFLINE — always)     │  │  Ktor Client    │ │
│  │  Ayahs · Tafsir · Hadith             │  │  (online only)  │ │
│  │  Bookmarks · Highlights · Notes      │  └────────┬────────┘ │
│  └───────────────────────────────────────┘           │          │
│                                                       │          │
│  androidMain/actual    iosMain/actual(stub)           │          │
│  GPS · Compass         GPS(stub) · Compass(stub)      │          │
│  Notifications         Notifications(stub)            │          │
└───────────────────────────────────────────────────────┼──────────┘
                                                        │ HTTPS
                                          ┌─────────────▼──────────────┐
                                          │   Azure VPS                 │
                                          │   Nginx (reverse proxy/SSL) │
                                          │                             │
                                          │  ┌──────────────────────┐  │
                                          │  │  FastAPI (Docker)    │  │
                                          │  │  Custom RAG Pipeline │  │
                                          │  │  multilingual-e5     │  │
                                          │  │  Claude Sonnet API   │  │
                                          │  └──────────┬───────────┘  │
                                          │             │               │
                                          │  ┌──────────▼───────────┐  │
                                          │  │ PostgreSQL+pgvector  │  │
                                          │  │ (Docker)             │  │
                                          │  │ Ayah embeddings      │  │
                                          │  │ Hadith embeddings    │  │
                                          │  │ Tafsir embeddings    │  │
                                          │  └──────────────────────┘  │
                                          └────────────────────────────┘
```

---

## 2. Complete Tech Stack

### Mobile (Compose Multiplatform)

| Concern | Library | Version |
|---|---|---|
| UI framework | Compose Multiplatform | 1.6.x |
| Language | Kotlin K2 | 2.0.x |
| Navigation | Voyager | 1.0.x |
| DI | Koin | 3.5.x |
| On-device DB | SQLDelight | 2.x |
| HTTP client | Ktor Client | 2.x |
| JSON | kotlinx.serialization | 1.6.x |
| Async | Kotlinx Coroutines | 1.8.x |
| Settings/prefs | Multiplatform Settings | 1.1.x |
| Prayer times | adhan-kotlin | 1.2.x |
| Testing (logic) | kotlin.test + MockK + coroutines-test | commonTest |
| Testing (UI) | compose-ui-test | androidTest |

### Platform-Specific (expect/actual)

| Feature | androidMain | iosMain (MVP) |
|---|---|---|
| GPS Location | FusedLocationProviderClient | Stub — null |
| Compass/Magnetometer | SensorManager | Stub — 0f |
| Notifications / Azan alarm | NotificationManager + AlarmManager | Stub |
| DB driver | AndroidSqliteDriver | NativeSqliteDriver |
| Foreground service (Azan) | ForegroundService | Stub |

### Backend (FastAPI)

| Concern | Library | Notes |
|---|---|---|
| API framework | FastAPI 0.111+ | Async, Pydantic v2 |
| Embedding model | sentence-transformers (`multilingual-e5-small`) | 384-dim, runs in container |
| Vector + Relational DB | PostgreSQL 16 + pgvector | Single self-hosted instance |
| ORM | SQLAlchemy 2.x + asyncpg | Async |
| LLM | Anthropic SDK (`claude-sonnet-4-20250514`) | Direct, no wrapper |
| HTTP server | Uvicorn | ASGI |
| Testing | pytest + pytest-asyncio + httpx | TDD |
| Linting | ruff + mypy | |

### Infrastructure

| Service | What Runs On It |
|---|---|
| Azure VPS (B2s min — 2 vCPU, 4GB RAM) | All Docker containers |
| Docker Compose | FastAPI + PostgreSQL + Nginx |
| Nginx | Reverse proxy + SSL termination |
| Let's Encrypt (Certbot) | Free SSL certificate |
| GitHub Actions | CI/CD — KMP tests + backend tests |

---

## 3. Infrastructure & Deployment

### Docker Compose Services

```
Azure VPS
└── Docker Compose
    ├── nginx          ← reverse proxy, SSL, routes /api → fastapi
    ├── fastapi        ← RAG pipeline, /chat/ endpoint
    └── postgres       ← PostgreSQL 16 + pgvector extension
```

### Port Layout
```
Public:  443 (HTTPS) → Nginx
Public:  80  (HTTP)  → Nginx (redirects to 443)
Internal: 8000       → FastAPI
Internal: 5432       → PostgreSQL (never exposed publicly)
```

### VPS Minimum Specs
| Resource | Minimum | Recommended |
|---|---|---|
| CPU | 2 vCPU | 2 vCPU |
| RAM | 4 GB | 8 GB |
| Storage | 20 GB SSD | 40 GB SSD |
| OS | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |

**Why 4GB minimum:** multilingual-e5-small loads ~500MB into RAM. PostgreSQL needs ~500MB. FastAPI + Uvicorn workers need ~500MB each. Nginx is negligible. 4GB gives comfortable headroom.

---

## 4. Project Structure

```
quran-app/
│
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/quranapp/
│       │   ├── ui/
│       │   │   ├── navigation/
│       │   │   │   ├── AppNavigation.kt
│       │   │   │   └── TabDestinations.kt
│       │   │   ├── screens/
│       │   │   │   ├── quran/
│       │   │   │   │   ├── QuranHomeScreen.kt       ← Surah list
│       │   │   │   │   ├── QuranReaderScreen.kt     ← Page/Scroll reader
│       │   │   │   │   └── components/
│       │   │   │   │       ├── MushafPageView.kt    ← Page-by-page layout
│       │   │   │   │       ├── QuranScrollView.kt   ← Continuous scroll
│       │   │   │   │       ├── AyahItem.kt          ← Single Ayah in scroll
│       │   │   │   │       ├── TafsirBottomSheet.kt ← 3-tab Tafsir panel
│       │   │   │   │       └── AnnotationMenu.kt    ← Long-press menu
│       │   │   │   ├── hadith/
│       │   │   │   │   ├── HadithCollectionsScreen.kt
│       │   │   │   │   ├── HadithBooksScreen.kt
│       │   │   │   │   ├── HadithListScreen.kt
│       │   │   │   │   └── components/
│       │   │   │   │       └── HadithCard.kt
│       │   │   │   ├── search/
│       │   │   │   │   ├── SearchScreen.kt
│       │   │   │   │   └── components/
│       │   │   │   │       └── SearchResultCard.kt
│       │   │   │   ├── chatbot/
│       │   │   │   │   ├── ChatbotScreen.kt
│       │   │   │   │   └── components/
│       │   │   │   │       ├── ChatBubble.kt
│       │   │   │   │       └── SourceCard.kt
│       │   │   │   └── settings/
│       │   │   │       ├── SettingsScreen.kt
│       │   │   │       ├── PrayerTimesScreen.kt
│       │   │   │       └── QiblaScreen.kt
│       │   │   └── theme/
│       │   │       ├── Theme.kt           ← Dark + Light MaterialTheme
│       │   │       ├── Color.kt
│       │   │       └── Type.kt            ← Uthmani + app fonts
│       │   │
│       │   ├── viewmodel/
│       │   │   ├── QuranViewModel.kt
│       │   │   ├── HadithViewModel.kt
│       │   │   ├── SearchViewModel.kt
│       │   │   ├── ChatbotViewModel.kt
│       │   │   ├── PrayerTimesViewModel.kt
│       │   │   ├── QiblaViewModel.kt
│       │   │   └── SettingsViewModel.kt
│       │   │
│       │   ├── domain/
│       │   │   ├── model/
│       │   │   │   ├── Ayah.kt
│       │   │   │   ├── Surah.kt
│       │   │   │   ├── TafsirEntry.kt
│       │   │   │   ├── Hadith.kt
│       │   │   │   ├── Bookmark.kt
│       │   │   │   ├── Highlight.kt
│       │   │   │   ├── Note.kt
│       │   │   │   ├── PrayerTimes.kt
│       │   │   │   ├── NextPrayer.kt
│       │   │   │   ├── ChatMessage.kt
│       │   │   │   ├── SearchResult.kt
│       │   │   │   └── QuranScript.kt     ← enum: HAFS, WARSH
│       │   │   ├── repository/            ← interfaces only
│       │   │   │   ├── QuranRepository.kt
│       │   │   │   ├── HadithRepository.kt
│       │   │   │   ├── UserDataRepository.kt
│       │   │   │   ├── SearchRepository.kt
│       │   │   │   └── ChatbotRepository.kt
│       │   │   └── usecase/
│       │   │       ├── quran/
│       │   │       │   ├── GetSurahListUseCase.kt
│       │   │       │   ├── GetAyahsForPageUseCase.kt   ← Mushaf page
│       │   │       │   ├── GetAyahsBySurahUseCase.kt   ← Scroll mode
│       │   │       │   └── GetTafsirUseCase.kt
│       │   │       ├── hadith/
│       │   │       │   ├── GetCollectionsUseCase.kt
│       │   │       │   └── GetHadithByBookUseCase.kt
│       │   │       ├── search/
│       │   │       │   └── SearchUseCase.kt
│       │   │       ├── userdata/
│       │   │       │   ├── ToggleBookmarkUseCase.kt
│       │   │       │   ├── SetHighlightUseCase.kt
│       │   │       │   └── SaveNoteUseCase.kt
│       │   │       ├── prayer/
│       │   │       │   ├── GetPrayerTimesUseCase.kt
│       │   │       │   └── SchedulePrayerAlarmsUseCase.kt
│       │   │       ├── qibla/
│       │   │       │   └── GetQiblaDirectionUseCase.kt
│       │   │       └── chatbot/
│       │   │           └── SendChatMessageUseCase.kt
│       │   │
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── QuranLocalDataSource.kt
│       │   │   │   ├── HadithLocalDataSource.kt
│       │   │   │   ├── SearchLocalDataSource.kt
│       │   │   │   └── UserDataLocalDataSource.kt
│       │   │   ├── remote/
│       │   │   │   ├── ChatbotRemoteDataSource.kt
│       │   │   │   └── dto/
│       │   │   │       ├── ChatRequestDto.kt
│       │   │   │       └── ChatResponseDto.kt
│       │   │   └── repository/
│       │   │       ├── QuranRepositoryImpl.kt
│       │   │       ├── HadithRepositoryImpl.kt
│       │   │       ├── SearchRepositoryImpl.kt
│       │   │       ├── UserDataRepositoryImpl.kt
│       │   │       └── ChatbotRepositoryImpl.kt
│       │   │
│       │   ├── di/
│       │   │   ├── AppModule.kt
│       │   │   ├── DatabaseModule.kt
│       │   │   ├── NetworkModule.kt
│       │   │   ├── RepositoryModule.kt
│       │   │   └── ViewModelModule.kt
│       │   │
│       │   └── util/
│       │       ├── PlatformContext.kt    ← expect/actual
│       │       ├── DatabaseDriver.kt     ← expect/actual
│       │       ├── LocationProvider.kt   ← expect/actual
│       │       ├── CompassSensor.kt      ← expect/actual
│       │       └── NotificationScheduler.kt ← expect/actual
│       │
│       ├── commonTest/kotlin/com/quranapp/
│       │   ├── data/local/
│       │   ├── domain/usecase/
│       │   └── viewmodel/
│       │
│       ├── androidMain/kotlin/com/quranapp/
│       │   ├── actual/                   ← GPS, compass, notifications
│       │   └── MainActivity.kt
│       │
│       ├── androidTest/kotlin/com/quranapp/
│       │   └── ui/                       ← Compose UI tests
│       │
│       └── iosMain/kotlin/com/quranapp/
│           └── actual/                   ← stubs only
│
├── backend/
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── dependencies.py
│   │   ├── rag/
│   │   │   ├── embedder.py
│   │   │   ├── retriever.py
│   │   │   ├── prompt_builder.py
│   │   │   └── llm_client.py
│   │   ├── routers/
│   │   │   └── chat.py
│   │   ├── models/
│   │   │   └── db_models.py
│   │   └── schemas/
│   │       └── chat.py
│   ├── ingestion/
│   │   ├── ingest_ayahs.py
│   │   ├── ingest_tafsir.py
│   │   └── ingest_hadith.py
│   ├── tests/
│   ├── requirements.txt
│   └── Dockerfile
│
├── nginx/
│   ├── nginx.conf
│   └── ssl/                              ← Let's Encrypt certs (gitignored)
│
├── data/
│   ├── ayahs/
│   ├── tafsir/
│   │   ├── 2-TafsirIbnAbbasInEnglish/
│   │   ├── 3-Maarif Ul Quran English/
│   │   └── 4-Ibn Kathir English/
│   └── hadith/
│       ├── Sahih_Bukhari/
│       ├── Sahih_Muslim/
│       ├── Sunan_Abu_Dawood/
│       ├── Jami_at_Tirmidhi/
│       ├── Sunan_an_Nasai/
│       └── Sunan_Ibn_Majah/
│
├── scripts/
│   ├── audit_jsons.py
│   └── build_sqlite.py
│
├── docker-compose.yml
├── .env.example
└── .github/
    └── workflows/
        ├── android-ci.yml
        └── backend-ci.yml
```

---

## 5. TDD Rules

```
Red   → Write failing test first
Green → Write minimum code to pass
Refactor → Clean up, keep tests green
```

| Layer | Test Location | Tool | Run Command |
|---|---|---|---|
| Use Cases | commonTest | kotlin.test + MockK | `./gradlew :composeApp:testDebugUnitTest` |
| ViewModels | commonTest | kotlin.test + coroutines-test | same |
| SQLDelight queries | commonTest | kotlin.test + JdbcSqliteDriver(IN_MEMORY) | same |
| Composables | androidTest | compose-ui-test | `./gradlew :composeApp:connectedAndroidTest` |
| FastAPI endpoints | backend/tests | pytest + httpx | `pytest` |
| RAG components | backend/tests | pytest + MockK-style mocks | `pytest` |

**CI rule:** Every PR must pass `testDebugUnitTest` and `pytest`. Failing test = blocked merge.

---

## 6. Phase 0 — Environment & CI Setup

**Goal:** Working KMP project, Docker stack running, CI green, smoke tests passing.
**Estimated time:** 2 days

### 6.1 KMP Project (Android Studio / Antigravity)
```bash
# Use KMP Wizard: File → New → KMP App
# Package: com.quranapp
# Targets: Android + iOS
# UI: Compose Multiplatform
```

### 6.2 Docker Compose

```yaml
# docker-compose.yml
version: '3.9'

services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: quran_postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - quran_network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  fastapi:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: quran_fastapi
    restart: unless-stopped
    environment:
      DATABASE_URL: postgresql+asyncpg://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres:5432/${POSTGRES_DB}
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      MODEL_NAME: intfloat/multilingual-e5-small
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - quran_network
    volumes:
      - model_cache:/root/.cache/huggingface

  nginx:
    image: nginx:alpine
    container_name: quran_nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - fastapi
    networks:
      - quran_network

volumes:
  postgres_data:
  model_cache:

networks:
  quran_network:
    driver: bridge
```

### 6.3 Nginx Config

```nginx
# nginx/nginx.conf
events { worker_connections 1024; }

http {
    upstream fastapi {
        server fastapi:8000;
    }

    # Redirect HTTP → HTTPS
    server {
        listen 80;
        server_name your-domain.com;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl;
        server_name your-domain.com;

        ssl_certificate     /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_protocols       TLSv1.2 TLSv1.3;

        # Rate limiting — protect chatbot endpoint
        limit_req_zone $binary_remote_addr zone=chat:10m rate=10r/m;

        location /api/ {
            limit_req zone=chat burst=5 nodelay;
            proxy_pass http://fastapi/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_read_timeout 60s;
        }

        location /api/health {
            proxy_pass http://fastapi/chat/health;
        }
    }
}
```

### 6.4 Backend Dockerfile

```dockerfile
# backend/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install system deps for sentence-transformers
RUN apt-get update && apt-get install -y \
    gcc g++ curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download embedding model at build time
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('intfloat/multilingual-e5-small')"

COPY . .

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
```

### 6.5 .env.example

```env
POSTGRES_USER=quranapp
POSTGRES_PASSWORD=change_this_strong_password
POSTGRES_DB=qurandb
ANTHROPIC_API_KEY=sk-ant-...
DOMAIN=your-domain.com
```

### 6.6 GitHub Actions

```yaml
# .github/workflows/android-ci.yml
name: Android CI
on: [push, pull_request]
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: ./gradlew :composeApp:testDebugUnitTest
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: composeApp/build/reports/tests/
```

```yaml
# .github/workflows/backend-ci.yml
name: Backend CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: '3.11' }
      - run: pip install -r backend/requirements.txt
      - run: cd backend && pytest --cov=app -v
```

### ✅ Phase 0 Exit Criteria
- [ ] `./gradlew :composeApp:testDebugUnitTest` passes smoke test
- [ ] `pytest` passes smoke test
- [ ] `docker compose up` starts all 3 services without error
- [ ] Nginx serves HTTPS on port 443
- [ ] `/api/health` returns `{"status": "ok"}`
- [ ] Both GitHub Actions CI pipelines pass

---

## 7. Phase 1 — Data Audit

**Goal:** All JSON data validated before writing a single line of app code.
**Estimated time:** 2–3 days

### 7.1 Audit Script

```python
# scripts/audit_jsons.py
import json, pytest
from pathlib import Path

def audit_ayahs(path: str) -> bool:
    with open(path, encoding='utf-8') as f: data = json.load(f)
    required = ['surah_number', 'ayah_number', 'arabic_text_hafs',
                'arabic_text_warsh', 'translation_english']
    issues = []
    for i, a in enumerate(data):
        for k in required:
            if k not in a: issues.append(f"Row {i}: missing {k}")
        if not a.get('arabic_text_hafs','').strip():
            issues.append(f"Row {i}: empty hafs text")
    surahs = {a['surah_number'] for a in data}
    if surahs != set(range(1,115)):
        issues.append(f"Missing surahs: {set(range(1,115)) - surahs}")
    print(f"Ayahs: {len(data)} | Issues: {len(issues)}")
    for i in issues[:10]: print(f"  ⚠  {i}")
    return len(issues) == 0
```

### 7.2 TDD Tests — Data Quality

```python
# backend/tests/test_data_quality.py
import json, pytest
from pathlib import Path

@pytest.fixture(scope='module')
def ayahs():
    with open('data/ayahs/quran.json', encoding='utf-8') as f:
        return json.load(f)

class TestAyahs:
    def test_total_count(self, ayahs):
        assert len(ayahs) == 6236

    def test_all_114_surahs(self, ayahs):
        assert {a['surah_number'] for a in ayahs} == set(range(1, 115))

    def test_no_empty_translation(self, ayahs):
        assert all(a.get('translation_english','').strip() for a in ayahs)

    def test_al_fatiha_7_ayahs(self, ayahs):
        assert len([a for a in ayahs if a['surah_number'] == 1]) == 7

@pytest.mark.parametrize("book,folder", [
    ('ibn_kathir',    '4-Ibn Kathir English'),
    ('maarif',        '3-Maarif Ul Quran English'),
    ('ibn_abbas',     '2-TafsirIbnAbbasInEnglish'),
])
class TestTafsir:
    def test_file_exists(self, book, folder):
        assert Path(f'data/tafsir/{folder}').exists()

    def test_has_entries(self, book, folder):
        files = list(Path(f'data/tafsir/{folder}').glob('*.json'))
        assert len(files) > 0

    def test_covers_minimum_ayahs(self, book, folder):
        all_entries = []
        for f in Path(f'data/tafsir/{folder}').glob('*.json'):
            with open(f, encoding='utf-8') as fp:
                all_entries.extend(json.load(fp))
        refs = {(e['surah_number'], e['ayah_number']) for e in all_entries}
        assert len(refs) >= 6000, f"{book} covers only {len(refs)} ayahs"

@pytest.mark.parametrize("collection,folder", [
    ('bukhari',   'Sahih_Bukhari'),
    ('muslim',    'Sahih_Muslim'),
    ('abu_dawud', 'Sunan_Abu_Dawood'),
    ('tirmidhi',  'Jami_at_Tirmidhi'),
    ('nasai',     'Sunan_an_Nasai'),
    ('ibn_majah', 'Sunan_Ibn_Majah'),
])
class TestHadith:
    def test_folder_exists(self, collection, folder):
        assert Path(f'data/hadith/{folder}').exists()

    def test_has_entries(self, collection, folder):
        files = list(Path(f'data/hadith/{folder}').glob('*.json'))
        assert len(files) > 0

    def test_required_fields(self, collection, folder):
        for f in Path(f'data/hadith/{folder}').glob('*.json'):
            with open(f, encoding='utf-8') as fp:
                data = json.load(fp)
            for h in data[:50]:
                assert 'hadith_number' in h
                assert h.get('translation','').strip()
```

### ✅ Phase 1 Exit Criteria
- [ ] TestAyahs — all 4 tests pass
- [ ] TestTafsir — all 3 books × 3 tests = 9 tests pass
- [ ] TestHadith — all 6 collections × 3 tests = 18 tests pass
- [ ] SQLite DB built: `python scripts/build_sqlite.py`
- [ ] DB size logged and acceptable (target < 500MB)

---

## 8. Phase 2 — SQLDelight Schema & Offline DB

**Goal:** Type-safe offline DB layer. All queries tested with in-memory SQLite driver.
**Estimated time:** 3–4 days

### SQLDelight Tables Summary

| Table | Content |
|---|---|
| `surahs` | Surah metadata (name Arabic/English, ayah count, revelation type) |
| `quran_pages` | Page-to-Ayah mapping for Mushaf page view |
| `ayahs` | Arabic (Hafs + Warsh), English translation, page/juz/hizb refs |
| `tafsir` | Ibn Kathir + Maarif + Ibn Abbas, keyed by surah+ayah+book |
| `hadith` | 6 collections, book number, hadith number, translation |
| `bookmarks` | type (ayah/hadith) + reference_id |
| `highlights` | ayah_id + color |
| `notes` | type + reference_id + content |
| `reading_position` | last read surah, ayah, page — one row |
| `settings` | script preference (hafs/warsh), theme, reading mode |

### Key Schema Additions vs Previous Plan

```sql
-- Mushaf page mapping — critical for page-by-page mode
CREATE TABLE quran_pages (
    page_number  INTEGER NOT NULL,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    PRIMARY KEY (surah_number, ayah_number)
);

-- Ayahs with both scripts
CREATE TABLE ayahs (
    id                   INTEGER PRIMARY KEY,
    surah_number         INTEGER NOT NULL,
    ayah_number          INTEGER NOT NULL,
    page_number          INTEGER NOT NULL,
    juz_number           INTEGER NOT NULL,
    arabic_text_hafs     TEXT NOT NULL,
    arabic_text_warsh    TEXT NOT NULL,
    translation_english  TEXT NOT NULL,
    UNIQUE(surah_number, ayah_number)
);

-- Reading position (resumed on next open)
CREATE TABLE reading_position (
    id           INTEGER PRIMARY KEY CHECK(id = 1),
    surah_number INTEGER NOT NULL DEFAULT 1,
    ayah_number  INTEGER NOT NULL DEFAULT 1,
    page_number  INTEGER NOT NULL DEFAULT 1,
    mode         TEXT NOT NULL DEFAULT 'scroll'
);

-- App settings
CREATE TABLE app_settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
-- Keys: 'quran_script' (hafs|warsh), 'theme' (dark|light), 'reading_mode' (page|scroll)
```

### Key SQLDelight Queries

```sql
-- Ayahs.sq
selectByPage:
SELECT * FROM ayahs WHERE page_number = :pageNumber ORDER BY surah_number, ayah_number;

selectBySurah:
SELECT * FROM ayahs WHERE surah_number = :surahNumber ORDER BY ayah_number;

selectArabicTextHafs:
SELECT arabic_text_hafs FROM ayahs WHERE surah_number = :s AND ayah_number = :a;

selectArabicTextWarsh:
SELECT arabic_text_warsh FROM ayahs WHERE surah_number = :s AND ayah_number = :a;

-- ReadingPosition.sq
getPosition:
SELECT * FROM reading_position WHERE id = 1;

upsertPosition:
INSERT INTO reading_position(id, surah_number, ayah_number, page_number, mode)
VALUES(1, :surah, :ayah, :page, :mode)
ON CONFLICT(id) DO UPDATE SET
    surah_number = excluded.surah_number,
    ayah_number  = excluded.ayah_number,
    page_number  = excluded.page_number,
    mode         = excluded.mode;
```

### TDD Tests — SQLDelight (commonTest)

```kotlin
// commonTest/kotlin/com/quranapp/data/local/QuranLocalDataSourceTest.kt
class QuranLocalDataSourceTest {
    private lateinit var db: QuranDatabase

    @BeforeTest fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        QuranDatabase.Schema.create(driver)
        db = QuranDatabase(driver)
        // Seed test data
        db.ayahsQueries.insert(1L,1L,1L,1L,1L,
            "بِسْمِ اللَّهِ","بِسْمِ اللَّهِ","In the name of Allah")
        db.ayahsQueries.insert(2L,1L,2L,1L,1L,
            "الْحَمْدُ لِلَّهِ","الْحَمْدُ لِلَّهِ","All praise is for Allah")
    }

    @Test fun `selectByPage returns correct ayahs`() {
        val ayahs = db.ayahsQueries.selectByPage(1L).executeAsList()
        assertEquals(2, ayahs.size)
    }

    @Test fun `selectBySurah returns ayahs in order`() {
        val ayahs = db.ayahsQueries.selectBySurah(1L).executeAsList()
        assertEquals(1L, ayahs[0].ayah_number)
        assertEquals(2L, ayahs[1].ayah_number)
    }

    @Test fun `upsertPosition saves and retrieves`() {
        db.readingPositionQueries.upsertPosition(2L, 255L, 29L, "scroll")
        val pos = db.readingPositionQueries.getPosition().executeAsOne()
        assertEquals(2L, pos.surah_number)
        assertEquals(255L, pos.ayah_number)
        assertEquals("scroll", pos.mode)
    }

    @Test fun `search returns matching translation`() {
        val results = db.ayahsQueries.search("%Allah%").executeAsList()
        assertTrue(results.isNotEmpty())
    }
}
```

### ✅ Phase 2 Exit Criteria
- [ ] All `.sq` files compile, SQLDelight generates Kotlin APIs
- [ ] QuranLocalDataSourceTest — 4+ tests pass
- [ ] UserDataLocalDataSourceTest — 10+ tests pass (bookmarks, highlights, notes)
- [ ] HadithLocalDataSourceTest — 4+ tests pass
- [ ] ReadingPositionTest — position saves and restores correctly
- [ ] `quran.db` bundled in `androidMain/assets/`

---

## 9. Phase 3 — Domain Layer & Use Cases

**Goal:** Pure Kotlin business logic. Zero Android imports in commonMain.
**Estimated time:** 2–3 days

### Domain Models (Key additions for this app)

```kotlin
// QuranScript.kt
enum class QuranScript { HAFS, WARSH }

// ReadingMode.kt
enum class ReadingMode { PAGE, SCROLL }

// Ayah.kt
data class Ayah(
    val id: Long,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val arabicTextHafs: String,
    val arabicTextWarsh: String,
    val translationEnglish: String
) {
    fun arabicText(script: QuranScript) = when(script) {
        QuranScript.HAFS -> arabicTextHafs
        QuranScript.WARSH -> arabicTextWarsh
    }
}

// SearchResult.kt
sealed class SearchResult {
    data class AyahResult(val ayah: Ayah, val matchedText: String) : SearchResult()
    data class HadithResult(val hadith: Hadith, val matchedText: String) : SearchResult()
}
```

### Use Case Tests (commonTest)

```kotlin
// GetAyahsForPageUseCaseTest.kt
class GetAyahsForPageUseCaseTest {
    private val repo: QuranRepository = mockk()
    private val useCase = GetAyahsForPageUseCase(repo)

    @Test fun `valid page returns ayahs`() = runTest {
        coEvery { repo.getAyahsByPage(1) } returns listOf(fakeAyah)
        val result = useCase(1)
        assertTrue(result.isSuccess)
    }

    @Test fun `page 0 returns failure`() = runTest {
        val result = useCase(0)
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test fun `page above 604 returns failure`() = runTest {
        val result = useCase(605)
        assertTrue(result.isFailure)
    }
}

// SearchUseCaseTest.kt
class SearchUseCaseTest {
    private val repo: SearchRepository = mockk()
    private val useCase = SearchUseCase(repo)

    @Test fun `empty query returns failure`() = runTest {
        val result = useCase("")
        assertTrue(result.isFailure)
    }

    @Test fun `query under 3 chars returns failure`() = runTest {
        val result = useCase("al")
        assertTrue(result.isFailure)
    }

    @Test fun `valid query returns combined results`() = runTest {
        coEvery { repo.searchAyahs("prayer") } returns listOf(fakeAyahResult)
        coEvery { repo.searchHadith("prayer") } returns listOf(fakeHadithResult)
        val result = useCase("prayer")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }
}
```

### ✅ Phase 3 Exit Criteria
- [ ] All domain models compile with zero Android imports
- [ ] GetAyahsForPageUseCase tests pass (3 tests)
- [ ] GetAyahsBySurahUseCase tests pass (4 tests)
- [ ] SearchUseCase tests pass (3 tests)
- [ ] GetTafsirUseCase tests pass (3 tests)
- [ ] All ViewModel tests pass (QuranViewModel, HadithViewModel, SearchViewModel)

---

## 10. Phase 4 — Quran Reader UI

**Goal:** Full Quran reading experience — both page-by-page Mushaf layout and continuous scroll, with Tafsir panel.
**Estimated time:** 7–10 days (most complex UI phase)

### Layout Architecture

```
QuranReaderScreen
├── TopBar
│   ├── SurahName + JuzNumber
│   ├── LayoutToggle (page/scroll icon)
│   └── ScriptToggle (Hafs/Warsh)
│
├── if readingMode == PAGE:
│   └── MushafPageView
│       ├── HorizontalPager (swipe left/right between pages)
│       ├── Each page: AyahLine items positioned to match Mushaf
│       └── AyahLine: Arabic text right-aligned, Bismillah header
│
└── if readingMode == SCROLL:
    └── QuranScrollView
        ├── LazyColumn of AyahItem composables
        ├── AyahItem: Arabic + translation (if enabled) + action buttons
        └── StickyHeader: Surah name + Bismillah per surah boundary

TafsirBottomSheet (slides up on Tafsir tap)
├── TabRow: [Ibn Kathir] [Maarif] [Ibn Abbas]
└── Pager: TabPage for each book
    └── Scrollable tafsir text

AnnotationMenu (appears on long press)
├── Bookmark toggle
├── Highlight color picker (5 colors)
└── Add Note
```

### Mushaf Page View — Critical Design Notes

The Mushaf page layout is the hardest UI component. Each page of the Quran has exactly the right Ayahs to fill a standard page (604 pages total). The layout must:

1. Right-to-left text flow across the full page width
2. Ayahs flow continuously — mid-Ayah line breaks are acceptable
3. Surah headers (Basmallah) appear between Surahs
4. Page number shown at bottom center
5. Juz/Hizb marker shown at top

```kotlin
// MushafPageView.kt — key composable structure
@Composable
fun MushafPageView(
    pageNumber: Int,
    ayahs: List<Ayah>,
    script: QuranScript,
    showTranslation: Boolean,
    onAyahLongPress: (Ayah) -> Unit,
    onTafsirClick: (Ayah) -> Unit,
) {
    // HorizontalPager for swipe between pages
    // Each page renders ayahs in flowing RTL Arabic text
    // Uses custom layout to mimic Mushaf typography
    // Uthmani font, large size (22sp minimum)
    // Full-bleed Arabic, translation below each ayah if enabled
}
```

### Compose UI Tests — Quran Reader

```kotlin
// androidTest — QuranReaderScreenTest.kt
@Test fun quranReader_defaultShowsArabicText() {
    composeTestRule.setContent { QuranReaderScreen(surahNumber = 1) }
    composeTestRule.onNodeWithTag("arabic_text_1_1").assertIsDisplayed()
}

@Test fun quranReader_translationToggle_hidesTranslation() {
    composeTestRule.setContent { QuranReaderScreen(surahNumber = 1) }
    composeTestRule.onNodeWithTag("translation_toggle").performClick()
    composeTestRule.onNodeWithTag("translation_1_1").assertDoesNotExist()
}

@Test fun quranReader_switchToPageMode_showsPageView() {
    composeTestRule.setContent { QuranReaderScreen(surahNumber = 1) }
    composeTestRule.onNodeWithTag("layout_toggle").performClick()
    composeTestRule.onNodeWithTag("mushaf_page_view").assertIsDisplayed()
}

@Test fun quranReader_longPress_showsAnnotationMenu() {
    composeTestRule.setContent { QuranReaderScreen(surahNumber = 1) }
    composeTestRule.onNodeWithTag("ayah_1_1").performTouchInput { longClick() }
    composeTestRule.onNodeWithTag("annotation_menu").assertIsDisplayed()
}

@Test fun tafsirBottomSheet_showsThreeTabs() {
    composeTestRule.setContent { TafsirBottomSheet(surahNumber = 1, ayahNumber = 1) }
    composeTestRule.onNodeWithText("Ibn Kathir").assertIsDisplayed()
    composeTestRule.onNodeWithText("Maarif").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ibn Abbas").assertIsDisplayed()
}
```

### ✅ Phase 4 Exit Criteria
- [ ] All Quran reader UI tests pass (5+ tests)
- [ ] Page-by-page mode: swipe works, Ayahs display correctly on real device
- [ ] Scroll mode: continuous Ayah list renders smoothly
- [ ] Layout toggle switches between modes, position preserved
- [ ] Script toggle (Hafs/Warsh) updates all Arabic text immediately
- [ ] Translation toggle works in both modes
- [ ] Tafsir bottom sheet opens with 3 tabs, content loads from SQLDelight
- [ ] Long-press shows annotation menu
- [ ] Arabic renders RTL correctly on real Android device (API 26+)
- [ ] Reading position saved and restored when app reopens

---

## 11. Phase 5 — Hadith Browser UI

**Goal:** Full Hadith browsing — collections → books → scrollable Hadith list with search.
**Estimated time:** 3–4 days

### Layout
```
HadithCollectionsScreen
└── LazyColumn of CollectionCard (6 items)
    ├── Collection name (Sahih Bukhari etc.)
    ├── Hadith count badge
    └── Tap → HadithBooksScreen

HadithBooksScreen
└── LazyColumn of BookRow
    └── Tap → HadithListScreen

HadithListScreen
└── LazyColumn of HadithCard
    ├── Hadith number + collection reference
    ├── Arabic text (toggleable)
    ├── Translation text
    ├── Narrator name
    └── Long press → annotation menu
```

### Compose UI Tests

```kotlin
@Test fun collections_showsAllSix() {
    composeTestRule.setContent { HadithCollectionsScreen() }
    listOf("Bukhari","Muslim","Abu Dawood","Tirmidhi","Nasai","Ibn Majah").forEach {
        composeTestRule.onNodeWithText(it, substring = true).assertIsDisplayed()
    }
}

@Test fun hadithCard_showsTranslation() {
    composeTestRule.setContent { HadithCard(hadith = fakeHadith) }
    composeTestRule.onNodeWithText(fakeHadith.translation, substring = true).assertIsDisplayed()
}

@Test fun hadithCard_longPress_showsAnnotationMenu() {
    var triggered = false
    composeTestRule.setContent {
        HadithCard(hadith = fakeHadith, onLongPress = { triggered = true })
    }
    composeTestRule.onNodeWithTag("hadith_card").performTouchInput { longClick() }
    assertTrue(triggered)
}
```

### ✅ Phase 5 Exit Criteria
- [ ] All 6 collections visible and tappable
- [ ] Book list navigates correctly
- [ ] Hadith list scrolls smoothly for large books (Bukhari ~7500 Hadith)
- [ ] All HadithCard tests pass

---

## 12. Phase 6 — Bookmarks, Highlights & Notes

**Goal:** Full annotation system — long-press on any Ayah or Hadith saves to SQLDelight.
**Estimated time:** 2–3 days

SQLDelight tests written in Phase 2. This phase adds ViewModel + UI tests.

### ViewModel Tests (commonTest)

```kotlin
class UserDataViewModelTest {
    private val repo: UserDataRepository = mockk()
    private lateinit var vm: UserDataViewModel

    @Test fun `toggleBookmark calls repository`() = runTest {
        coEvery { repo.isBookmarked("ayah", 1L) } returns false
        coEvery { repo.toggleBookmark("ayah", 1L) } returns true
        vm.toggleBookmark("ayah", 1L)
        advanceUntilIdle()
        coVerify { repo.toggleBookmark("ayah", 1L) }
    }

    @Test fun `setHighlight updates state`() = runTest {
        coEvery { repo.setHighlight(50L, "#FFD700") } returns Unit
        vm.setHighlight(50L, "#FFD700")
        advanceUntilIdle()
        coVerify { repo.setHighlight(50L, "#FFD700") }
    }
}
```

### ✅ Phase 6 Exit Criteria
- [ ] Bookmark icon appears/disappears immediately on tap
- [ ] Highlight color visible on Ayah card after selection
- [ ] Notes modal opens, saves, and reloads correctly
- [ ] All persists after app restart

---

## 13. Phase 7 — Search

**Goal:** Unified offline search across Quran and Hadith simultaneously.
**Estimated time:** 2–3 days

### Search Architecture

```kotlin
// SearchUseCase — runs both queries in parallel
class SearchUseCase(private val repo: SearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchResult>> {
        if (query.trim().length < 3)
            return Result.failure(IllegalArgumentException("Query must be at least 3 characters"))
        return runCatching {
            coroutineScope {
                val ayahs = async { repo.searchAyahs(query) }
                val hadiths = async { repo.searchHadith(query) }
                ayahs.await().map { SearchResult.AyahResult(it, query) } +
                hadiths.await().map { SearchResult.HadithResult(it, query) }
            }
        }
    }
}
```

### Search Tests (commonTest)

```kotlin
@Test fun `search runs ayah and hadith queries in parallel`() = runTest {
    coEvery { repo.searchAyahs(any()) } returns listOf(fakeAyah)
    coEvery { repo.searchHadith(any()) } returns listOf(fakeHadith)
    val result = useCase("prayer")
    assertEquals(2, result.getOrNull()?.size)
}

@Test fun `search results show correct type labels`() = runTest {
    coEvery { repo.searchAyahs(any()) } returns listOf(fakeAyah)
    coEvery { repo.searchHadith(any()) } returns emptyList()
    val results = useCase("mercy").getOrNull()!!
    assertTrue(results.first() is SearchResult.AyahResult)
}
```

### ✅ Phase 7 Exit Criteria
- [ ] All search tests pass (3+ tests)
- [ ] Search results show Quran and Hadith results together
- [ ] Tapping a result navigates to correct Ayah or Hadith
- [ ] Empty state shown for no results
- [ ] Search query < 3 chars shows validation message

---

## 14. Phase 8 — Prayer Times, Notifications & Qibla

**Goal:** Accurate prayer times, custom Azan alarms per prayer, Qibla compass.
**Estimated time:** 3–4 days

### Prayer Times Tests (commonTest)

```kotlin
class GetPrayerTimesUseCaseTest {
    private val useCase = GetPrayerTimesUseCase()
    private val lahore = Coordinates(31.5204, 74.3587)
    private val date = LocalDate(2024, 6, 15)

    @Test fun `returns all five prayers`() {
        val r = useCase(lahore, date)
        assertTrue(r.isSuccess)
        assertNotNull(r.getOrNull()?.fajr)
        assertNotNull(r.getOrNull()?.isha)
    }

    @Test fun `prayers are chronological`() {
        val t = useCase(lahore, date).getOrNull()!!
        val times = listOf(t.fajr, t.dhuhr, t.asr, t.maghrib, t.isha)
        for (i in 1 until times.size) assertTrue(times[i] > times[i-1])
    }

    @Test fun `different locations give different times`() {
        val london = Coordinates(51.5074, -0.1278)
        assertNotEquals(
            useCase(lahore, date).getOrNull()!!.fajr,
            useCase(london, date).getOrNull()!!.fajr
        )
    }
}
```

### Notification Scheduling (androidMain actual)

```kotlin
// androidMain/actual/NotificationScheduler.kt
actual class NotificationScheduler(private val context: Context) {

    actual fun schedulePrayerAlarm(
        prayerName: String,
        timeEpochMillis: Long,
        customSoundUri: String?
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayerName)
            putExtra("custom_sound", customSoundUri)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, prayerName.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, timeEpochMillis, pendingIntent
        )
    }

    actual fun cancelPrayerAlarm(prayerName: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, prayerName.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}
```

### Qibla Tests (commonTest)

```kotlin
@Test fun `London to Mecca is ~119 degrees`() {
    assertEquals(119.0, useCase(51.5, -0.12), absoluteTolerance = 1.0)
}
@Test fun `New York to Mecca is ~59 degrees`() {
    assertEquals(59.0, useCase(40.71, -74.01), absoluteTolerance = 1.0)
}
@Test fun `bearing is between 0 and 360`() {
    val b = useCase(31.52, 74.36)
    assertTrue(b in 0.0..360.0)
}
```

### ✅ Phase 8 Exit Criteria
- [ ] All prayer time tests pass (3 tests)
- [ ] All Qibla tests pass (3 tests)
- [ ] Prayer times display correctly for current GPS location
- [ ] Custom alarm fires at prayer time (tested with 1 min offset)
- [ ] Each prayer has independent on/off toggle in settings
- [ ] Qibla compass rotates in real-time on real device
- [ ] Location denied → manual city entry fallback

---

## 15. Phase 9 — RAG Backend

**Goal:** Custom RAG pipeline, fully tested, deployed to Azure VPS via Docker.
**Estimated time:** 5–7 days

### Embedder

```python
# backend/app/rag/embedder.py
from sentence_transformers import SentenceTransformer
from functools import lru_cache

@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    return SentenceTransformer('intfloat/multilingual-e5-small')

def embed_query(text: str) -> list[float]:
    return get_model().encode(f"query: {text}", normalize_embeddings=True).tolist()

def embed_passage(text: str) -> list[float]:
    return get_model().encode(f"passage: {text}", normalize_embeddings=True).tolist()
```

### Retriever

```python
# backend/app/rag/retriever.py
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.rag.embedder import embed_query

MIN_SIM = 0.30
TOP_K = 5

async def retrieve(query: str, db: AsyncSession) -> dict:
    vec = embed_query(query)
    emb = f"[{','.join(map(str, vec))}]"
    p = {"emb": emb, "min_sim": MIN_SIM, "top_k": TOP_K}

    ayahs = await db.execute(text("""
        SELECT surah_number, ayah_number, content,
               1 - (embedding <=> :emb::vector) AS sim
        FROM ayah_embeddings
        WHERE 1 - (embedding <=> :emb::vector) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), p)

    hadiths = await db.execute(text("""
        SELECT collection, hadith_number, content,
               1 - (embedding <=> :emb::vector) AS sim
        FROM hadith_embeddings
        WHERE 1 - (embedding <=> :emb::vector) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), p)

    tafsir = await db.execute(text("""
        SELECT surah_number, ayah_number, book_name, content,
               1 - (embedding <=> :emb::vector) AS sim
        FROM tafsir_embeddings
        WHERE 1 - (embedding <=> :emb::vector) > :min_sim
        ORDER BY sim DESC LIMIT :top_k
    """), p)

    return {
        "ayahs":   [dict(r._mapping) for r in ayahs],
        "hadiths": [dict(r._mapping) for r in hadiths],
        "tafsir":  [dict(r._mapping) for r in tafsir],
    }
```

### Prompt Builder

```python
# backend/app/rag/prompt_builder.py
SYSTEM_PROMPT = """You are an Islamic knowledge assistant.
Answer using ONLY the provided references. Rules:
1. Cite every claim: (Surah X:Y) for Ayahs, (Collection, #N) for Hadith
2. If no reference answers the question, say so explicitly — never fabricate
3. Present multiple Tafsir perspectives when they differ
4. Keep language accessible for learners and non-Muslims
5. End every response: "For personal guidance, consult a qualified Islamic scholar."
"""

def build_prompt(query: str, retrieved: dict) -> list[dict]:
    parts = []
    if retrieved["ayahs"]:
        parts.append("=== QURAN VERSES ===")
        for a in retrieved["ayahs"]:
            parts.append(f"[{a['surah_number']}:{a['ayah_number']}]\n{a['content']}")
    if retrieved["hadiths"]:
        parts.append("\n=== HADITH ===")
        for h in retrieved["hadiths"]:
            parts.append(f"[{h['collection'].title()} #{h['hadith_number']}]\n{h['content']}")
    if retrieved["tafsir"]:
        parts.append("\n=== TAFSIR ===")
        for t in retrieved["tafsir"]:
            book = t['book_name'].replace('_',' ').title()
            parts.append(f"[{book} on {t['surah_number']}:{t['ayah_number']}]\n{t['content']}")
    if not parts:
        parts.append("No relevant references found.")
    return [{"role": "user", "content": f"References:\n{chr(10).join(parts)}\n\nQuestion: {query}"}]
```

### TDD Tests — Backend

```python
# tests/test_embedder.py
import numpy as np
from app.rag.embedder import embed_query, embed_passage

def test_dimension(): assert len(embed_query("test")) == 384
def test_normalized(): assert abs(np.linalg.norm(embed_query("test")) - 1.0) < 1e-5
def test_arabic(): assert len(embed_passage("بِسْمِ اللَّهِ")) == 384
def test_similar_closer():
    e1, e2, e3 = embed_query("prayer Islam"), embed_query("salah worship"), embed_query("pasta recipe")
    assert np.dot(e1, e2) > np.dot(e1, e3)

# tests/test_prompt_builder.py
from app.rag.prompt_builder import build_prompt, SYSTEM_PROMPT

def test_cites_ayah():
    r = {"ayahs":[{"surah_number":2,"ayah_number":153,"content":"Be patient"}],"hadiths":[],"tafsir":[]}
    assert any("2:153" in m["content"] for m in build_prompt("patience", r))

def test_empty_retrieval():
    r = {"ayahs":[],"hadiths":[],"tafsir":[]}
    assert any("No relevant" in m["content"] for m in build_prompt("test", r))

def test_system_has_citation_rule(): assert "Cite every claim" in SYSTEM_PROMPT
def test_system_has_disclaimer(): assert "qualified Islamic scholar" in SYSTEM_PROMPT

# tests/test_chat_router.py
@pytest.mark.asyncio
async def test_200_valid_message():
    with patch('app.routers.chat.retrieve', new_callable=AsyncMock) as mr, \
         patch('app.routers.chat.get_llm_response', new_callable=AsyncMock) as ml:
        mr.return_value = {"ayahs":[],"hadiths":[],"tafsir":[]}
        ml.return_value = "Answer here."
        async with AsyncClient(app=app, base_url="http://test") as c:
            r = await c.post("/api/chat/", json={"message": "What is Zakat?"})
        assert r.status_code == 200
        assert "answer" in r.json()

@pytest.mark.asyncio
async def test_400_empty(): ...
@pytest.mark.asyncio
async def test_400_too_long(): ...
@pytest.mark.asyncio
async def test_health(): ...
```

### Ingestion Scripts

```python
# ingestion/ingest_tafsir.py
TAFSIR_BOOKS = {
    'ibn_kathir':  'data/tafsir/4-Ibn Kathir English/',
    'maarif':      'data/tafsir/3-Maarif Ul Quran English/',
    'ibn_abbas':   'data/tafsir/2-TafsirIbnAbbasInEnglish/',
}

async def ingest_tafsir():
    for book_name, folder in TAFSIR_BOOKS.items():
        entries = load_json_folder(folder)
        for entry in entries:
            content = f"passage: {entry['content']}"
            embedding = embed_passage(content)
            await db.execute("""
                INSERT INTO tafsir_embeddings
                (surah_number, ayah_number, book_name, content, embedding)
                VALUES ($1,$2,$3,$4,$5)
            """, entry['surah_number'], entry['ayah_number'],
                book_name, entry['content'], embedding)
        print(f"✅ {book_name}: {len(entries)} entries ingested")
```

### ✅ Phase 9 Exit Criteria
- [ ] Embedder tests pass (4 tests)
- [ ] Retriever tests pass (4 tests)
- [ ] Prompt builder tests pass (4 tests)
- [ ] Chat router tests pass (4 tests)
- [ ] Ingestion: all Ayahs, 3 Tafsir books, 6 Hadith collections → pgvector
- [ ] Manual test: 15 representative Islamic queries return relevant results
- [ ] `docker compose up` on Azure VPS — all services healthy
- [ ] `https://your-domain.com/api/health` returns 200

---

## 16. Phase 10 — AI Chatbot UI

**Goal:** Chatbot screen with tappable citations that navigate to source Ayah or Hadith.
**Estimated time:** 3–4 days

### ChatbotViewModel Tests (commonTest)

```kotlin
@Test fun `initial state is empty`() {
    assertTrue(vm.uiState.value.messages.isEmpty())
    assertFalse(vm.uiState.value.isLoading)
}

@Test fun `user message appears immediately`() = runTest {
    coEvery { sendMessage(any()) } coAnswers { delay(100); Result.success(fakeResponse) }
    vm.sendMessage("What is Zakat?")
    advanceTimeBy(10)
    assertTrue(vm.uiState.value.messages.any { it.role == ChatRole.USER })
}

@Test fun `assistant response appears on success`() = runTest {
    coEvery { sendMessage(any()) } returns Result.success(fakeResponse)
    vm.sendMessage("What is Zakat?")
    advanceUntilIdle()
    assertTrue(vm.uiState.value.messages.any { it.role == ChatRole.ASSISTANT })
}

@Test fun `error message shown on network failure`() = runTest {
    coEvery { sendMessage(any()) } returns Result.failure(RuntimeException("No internet"))
    vm.sendMessage("test")
    advanceUntilIdle()
    assertTrue(vm.uiState.value.messages.last().content.contains("error", ignoreCase = true))
}

@Test fun `empty message is not sent`() = runTest {
    vm.sendMessage("   ")
    advanceUntilIdle()
    coVerify(exactly = 0) { sendMessage(any()) }
}
```

### Compose UI Tests

```kotlin
@Test fun chatScreen_showsDisclaimerBelowResponse() {
    composeTestRule.setContent { ChatBubble(message = fakeAssistantMessage) }
    composeTestRule.onNodeWithText("qualified Islamic scholar", substring = true).assertIsDisplayed()
}

@Test fun sourceCard_showsAyahReference() {
    composeTestRule.setContent { SourceCard(source = AyahReference(2, 153)) }
    composeTestRule.onNodeWithText("2:153", substring = true).assertIsDisplayed()
}

@Test fun chatScreen_offlineBanner_shownWhenOffline() {
    composeTestRule.setContent { ChatbotScreen(isOnline = false) }
    composeTestRule.onNodeWithTag("offline_banner").assertIsDisplayed()
}
```

### ✅ Phase 10 Exit Criteria
- [ ] ChatbotViewModel tests pass (5 tests)
- [ ] ChatBubble Compose tests pass (3 tests)
- [ ] SourceCard tappable, navigates to correct Ayah/Hadith
- [ ] Scholar disclaimer visible on every assistant message
- [ ] Offline banner shown when no internet
- [ ] Loading indicator visible during API call

---

## 17. Phase 11 — Theme, Polish & Play Store

**Goal:** Dark/Light mode, final performance testing, Play Store submission.
**Estimated time:** 3–4 days

### Theme Implementation

```kotlin
// Theme.kt
@Composable
fun QuranAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = QuranTypography, content = content)
}

// Dark — deep greens and golds (Islamic aesthetic)
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF7D),          // Emerald green
    onPrimary = Color(0xFF000000),
    background = Color(0xFF0D1117),       // Very dark
    surface = Color(0xFF161B22),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
)

// Light — cream and warm tones (like a physical Mushaf)
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2D6A4F),          // Deep green
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F0E8),       // Cream/parchment
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
)
```

### Performance Checklist
- [ ] Cold start < 3 seconds on mid-range Android (Pixel 4 equivalent)
- [ ] Surah list loads < 200ms
- [ ] Page swipe in Mushaf mode is 60fps (no jank)
- [ ] SQLDelight queries < 50ms
- [ ] Search results appear < 300ms
- [ ] Chatbot response < 8 seconds end-to-end

### Play Store Checklist
- [ ] App icon 512×512 PNG
- [ ] Feature graphic 1024×500 PNG
- [ ] Min 4 phone screenshots (Quran reader, Hadith, Chatbot, Prayer times)
- [ ] Short description (80 chars max)
- [ ] Privacy policy URL (required — uses GPS)
- [ ] Content rating completed
- [ ] Target SDK 34+
- [ ] 64-bit AAB: `./gradlew :composeApp:bundleRelease`

---

## 18. Database Schemas

### SQLDelight (On-Device — Full)

```sql
-- All tables in commonMain/sqldelight/com/quranapp/db/

-- surahs, quran_pages, ayahs, tafsir, hadith (content tables — read only)
-- bookmarks, highlights, notes (user data — read/write)
-- reading_position (single row — read/write)
-- app_settings (key/value — read/write)
```
(Full `.sq` files defined in Phase 2)

### PostgreSQL + pgvector (Backend — RAG only)

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE ayah_embeddings (
    id           SERIAL PRIMARY KEY,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    content      TEXT NOT NULL,
    embedding    vector(384) NOT NULL
);

CREATE TABLE hadith_embeddings (
    id            SERIAL PRIMARY KEY,
    collection    TEXT NOT NULL,
    hadith_number INTEGER NOT NULL,
    content       TEXT NOT NULL,
    embedding     vector(384) NOT NULL
);

CREATE TABLE tafsir_embeddings (
    id           SERIAL PRIMARY KEY,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    book_name    TEXT NOT NULL,
    content      TEXT NOT NULL,
    embedding    vector(384) NOT NULL
);

CREATE INDEX ON ayah_embeddings   USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON hadith_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON tafsir_embeddings USING hnsw (embedding vector_cosine_ops);
```

---

## 19. Docker & Deployment

### VPS Setup Steps (Azure B2s, Ubuntu 22.04)

```bash
# 1. Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 2. Install Docker Compose
sudo apt-get install docker-compose-plugin

# 3. Clone repo and configure
git clone https://github.com/your/quran-app.git
cd quran-app
cp .env.example .env
nano .env   # fill in real values

# 4. SSL with Certbot (before starting nginx)
sudo apt install certbot
sudo certbot certonly --standalone -d your-domain.com
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/

# 5. Start all services
docker compose up -d

# 6. Run ingestion (one-time)
docker compose exec fastapi python ingestion/ingest_ayahs.py
docker compose exec fastapi python ingestion/ingest_tafsir.py
docker compose exec fastapi python ingestion/ingest_hadith.py

# 7. Verify
curl https://your-domain.com/api/health
# Expected: {"status":"ok"}
```

### Deployment Flow (CI/CD)

```
Git push to main
     │
     ▼
GitHub Actions
├── Run pytest (backend tests)
├── Run KMP unit tests
└── If all pass:
    SSH to Azure VPS
    git pull
    docker compose up -d --build
    Health check: /api/health
```

---

## 20. API Contract

### POST /api/chat/

**Request:** `{ "message": "What does Islam say about patience?" }`

**Response (200):**
```json
{
  "answer": "Islam greatly emphasizes patience (Sabr). In Surah Al-Baqarah (2:153)...\n\nFor personal guidance, consult a qualified Islamic scholar.",
  "sources": {
    "ayahs":   [{ "surah": 2, "ayah": 153 }],
    "hadiths": [{ "collection": "bukhari", "number": 1469 }],
    "tafsir":  [{ "surah": 2, "ayah": 153, "book": "ibn_kathir" }]
  }
}
```

**Errors:**
```json
{ "detail": "Message cannot be empty" }                // 400
{ "detail": "Message too long (max 1000 characters)" } // 400
{ "detail": "AI service temporarily unavailable" }     // 503
```

### GET /api/health
```json
{ "status": "ok", "model_loaded": true, "db_connected": true }
```

---

## 21. RAG Pipeline Design

```
User: "What does Islam say about patience?"
         │
    embed_query()  →  384-dim vector
         │
    pgvector search (3 parallel queries)
    ├── ayah_embeddings    → top 5, sim > 0.30
    ├── hadith_embeddings  → top 5, sim > 0.30
    └── tafsir_embeddings  → top 5, sim > 0.30
         │
    build_prompt()
    ├── System prompt (citation rules + disclaimer)
    ├── Retrieved Ayahs with references
    ├── Retrieved Hadiths with references
    └── Retrieved Tafsir with book names
         │
    Claude Sonnet API
    temperature=0.2, max_tokens=1000
         │
    { answer, sources }  →  Ktor  →  App
```

---

## 22. Testing Strategy

| Phase | Tests | Location | Target |
|---|---|---|---|
| Data quality | 31 | backend/tests | 100% pass before Phase 2 |
| SQLDelight | 18 | commonTest | 90% coverage |
| Use cases | 25 | commonTest | 90% coverage |
| ViewModels | 20 | commonTest | 85% coverage |
| Composables | 22 | androidTest | 80% coverage |
| Backend RAG | 16 | backend/tests | 90% coverage |
| **Total** | **~132** | | |

---

## 23. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Azure B2s RAM insufficient | Medium | High | Monitor memory; upgrade to B4ms if embedding model causes OOM |
| Mushaf page layout incorrect | High | High | Source authoritative page-to-Ayah mapping from tanzil.net dataset |
| OCR errors in Tafsir/Hadith | Medium | High | Phase 1 audit tests are hard gates — fix data before proceeding |
| Arabic RTL rendering issues CMP | Medium | High | Test on real device Phase 4, not emulator |
| pgvector retrieval quality poor | Medium | High | Manual test 15 queries before Phase 10 |
| SSL cert renewal breaks nginx | Low | High | Add cron job: `certbot renew --pre-hook "docker stop nginx" --post-hook "docker start nginx"` |
| adhan-kotlin not fully CMP | Low | Medium | Verify in Phase 0 before building prayer feature |
| Chatbot answers controversial Fiqh | High | Medium | System prompt + scholar disclaimer enforced in tests |

---

## 24. MVP → Post-MVP Checklist

### MVP Delivers ✅
- Quran reader — page + scroll, Hafs + Warsh, translation toggle
- Tafsir — Ibn Kathir, Maarif, Ibn Abbas (3-tab panel)
- Hadith browser — 6 collections, offline
- Search — Quran + Hadith unified
- Bookmarks, highlights, notes
- Prayer times + custom Azan alarms + notifications
- Qibla compass
- AI Chatbot — custom RAG, tappable citations
- Dark + Light theme
- Play Store (Android)

### Post-MVP Priority Order
1. Word-by-word translation
2. Firebase Auth + cloud sync for annotations
3. Audio recitation + scroll sync
4. Additional Tafsir books (Maududi, Syed Qatab, Arabic books)
5. iOS `actual` implementations
6. Urdu translation option
7. Arabic root explorer
8. Multiple Quran translations (Pickthall, Yusuf Ali)

---
*Final MVP Plan — Version 1.0 | March 2026*
