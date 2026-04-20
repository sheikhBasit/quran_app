# Quran App

> Full Islamic reference app for Android with an AI-powered chatbot backed by a custom RAG pipeline.

Built with Compose Multiplatform (Android-first) and a FastAPI backend. The AI chatbot answers questions with cited Quran ayahs, Tafsir, and Hadith references — no hallucinated quotes.

---

## Features

- **Quran reader** — Mushaf page layout + continuous scroll, Hafs & Warsh scripts
- **Tafsir** — Ibn Kathir, Maarif ul Quran, Ibn Abbas (English)
- **Hadith browser** — 6 major collections (Bukhari, Muslim, Abu Dawood, Tirmidhi, Nasai, Ibn Majah)
- **AI Chatbot** — custom RAG pipeline with cited Ayah, Hadith & Tafsir references per answer
- **Prayer times** + custom Azan alarms
- **Qibla compass**
- **Unified search** — across Quran + Hadith simultaneously
- **Bookmarks, highlights, notes**
- **Dark + Light theme**

---

## Stack

| Layer | Tech |
|-------|------|
| Mobile | Compose Multiplatform (Android-first, iOS post-MVP) |
| Backend | FastAPI + Python 3.11 |
| AI / RAG | sentence-transformers · pgvector · Claude Sonnet API |
| Database | PostgreSQL (pgvector) · SQLite (on-device) |
| Deployment | Azure VPS · Docker Compose · Nginx |

---

## Quick Start

### Backend (Docker)

```bash
git clone https://github.com/sheikhBasit/quran_app
cd quran_app
cp .env.example .env        # add API keys
docker compose up -d        # starts FastAPI + PostgreSQL + Nginx

# First time only — ingest data
docker compose exec fastapi python ingestion/ingest_ayahs.py
docker compose exec fastapi python ingestion/ingest_tafsir.py
docker compose exec fastapi python ingestion/ingest_hadith.py

curl http://localhost/api/health
```

### Android App

```bash
./gradlew :composeApp:installDebug          # install on connected device
./gradlew :composeApp:testDebugUnitTest     # run unit tests
```

---

## Architecture

```
quran_app/
├── composeApp/         — KMP Android app (Compose Multiplatform)
│   └── src/
│       ├── androidMain/    — Android-specific code
│       └── commonMain/     — Shared UI + logic
├── backend/
│   ├── api/            — FastAPI routers
│   ├── rag/            — RAG pipeline (chunking, embeddings, reranking)
│   ├── ingestion/      — Data ingestion scripts
│   └── tests/          — pytest test suite
├── nginx/              — Reverse proxy config
├── data/               — Raw JSON (Ayahs, Tafsir, Hadith)
└── scripts/            — Data audit + SQLite build tools
```

---

## RAG Pipeline

The chatbot uses a custom retrieval pipeline — not off-the-shelf LangChain chains:

1. Query → embed with `sentence-transformers`
2. Hybrid search: pgvector similarity + keyword match across Quran, Tafsir, Hadith
3. Reranking: cross-encoder reranker filters top results
4. Claude Sonnet generates answer with explicit source citations
5. Every answer links back to exact Ayah, Hadith number, and Tafsir passage

---

## Testing

```bash
# Backend
pytest backend/tests/

# Android unit tests
./gradlew :composeApp:testDebugUnitTest

# Android instrumented tests
./gradlew :composeApp:connectedAndroidTest
```

---

Built by [Abdul Basit](https://github.com/sheikhBasit) · FastAPI · Compose Multiplatform · pgvector · Claude API · Azure


---

## Knowledge Graph

This repo is indexed by [Understand Anything](https://github.com/Lum1104/Understand-Anything) — a multi-agent pipeline that builds a knowledge graph of every file, function, class, and dependency.

The graph lives at `.understand-anything/knowledge-graph.json` and can be explored visually:

```bash
# In Claude Code, from this repo root:
/understand-dashboard
```

To rebuild the graph after major changes:

```bash
~/scripts/graphify-all.sh
```

> Graph covers: files · functions · classes · imports · architecture layers · plain-English summaries · guided tours.
