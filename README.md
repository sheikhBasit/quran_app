# Quran App

A comprehensive Islamic reference app with AI-powered chatbot.

## Features
- Quran reader — Mushaf page layout + continuous scroll, Hafs & Warsh scripts
- Tafsir — Ibn Kathir, Maarif ul Quran, Ibn Abbas (English)
- Hadith browser — 6 major collections (Bukhari, Muslim, Abu Dawood, Tirmidhi, Nasai, Ibn Majah)
- AI Chatbot — custom RAG pipeline with cited Ayah, Hadith & Tafsir references
- Bookmarks, highlights, notes
- Prayer times + custom Azan alarms
- Qibla compass
- Unified search (Quran + Hadith)
- Dark + Light theme

## Stack
- **Mobile:** Compose Multiplatform (Android-first, iOS post-MVP)
- **Backend:** FastAPI + custom RAG (sentence-transformers + pgvector + Claude Sonnet)
- **Deployment:** Azure VPS + Docker Compose + Nginx

## Quick Start

### Backend (Docker)
```bash
cp .env.example .env        # fill in values
docker compose up -d        # start all services
# Run ingestion (first time only):
docker compose exec fastapi python ingestion/ingest_ayahs.py
docker compose exec fastapi python ingestion/ingest_tafsir.py
docker compose exec fastapi python ingestion/ingest_hadith.py
curl http://localhost/api/health
```

### Data Audit
```bash
python scripts/audit_jsons.py   # verify data quality
python scripts/build_sqlite.py  # build on-device DB
```

### Mobile
```bash
./gradlew :composeApp:installDebug       # install on device
./gradlew :composeApp:testDebugUnitTest  # run unit tests
```

## Project Structure
```
Quran_App/
├── composeApp/    — KMP mobile app
├── backend/       — FastAPI + RAG pipeline
├── nginx/         — Reverse proxy config
├── data/          — Raw JSON (Ayahs, Tafsir, Hadith)
├── scripts/       — Data audit + SQLite build
├── docs/          — Project plan + Antigravity prompts
└── .skills/       — Custom Antigravity skills
```

## Development Methodology
**TDD throughout** — Red → Green → Refactor on every feature.
- Common unit tests: `./gradlew :composeApp:testDebugUnitTest`
- Backend tests: `pytest backend/tests/`
- UI tests: `./gradlew :composeApp:connectedAndroidTest`
