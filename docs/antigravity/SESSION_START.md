# Antigravity Session Start Template
# Copy this file's content at the beginning of EVERY Antigravity session.
# Then paste the relevant phase prompt from antigravity-prompts.md.

## Step 1 — Update Phase Status
Mark completed phases with [x] before pasting:

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

## Step 2 — Paste Skills File
Paste the entire contents of: docs/antigravity/antigravity-skills-final.md

## Step 3 — Paste Phase Prompt
Paste the relevant phase prompt from: docs/antigravity/antigravity-prompts.md

## Step 4 — Antigravity Protocol Reminder
Before Antigravity writes any code, it must:
1. Output a plan listing files to create/modify, tests to write, commands to run
2. Ask for your approval
3. Write failing test first (Red)
4. Implement minimum code to pass (Green)
5. Refactor and confirm tests still pass
6. Report: "X tests passing, 0 failing"

## Useful Commands to Verify Progress

### Mobile
```bash
# Run all unit tests (fast, no device needed)
./gradlew :composeApp:testDebugUnitTest

# Run with report
./gradlew :composeApp:testDebugUnitTest jacocoTestReport
open composeApp/build/reports/tests/testDebugUnitTest/index.html

# Install on device
./gradlew :composeApp:installDebug
```

### Backend
```bash
# All tests
cd backend && pytest -v

# Specific file
cd backend && pytest tests/test_embedder.py -v

# With coverage
cd backend && pytest --cov=app --cov-report=term-missing

# Data audit (run before Phase 2)
python scripts/audit_jsons.py

# Build SQLite DB
python scripts/build_sqlite.py
```

### Docker (local dev)
```bash
docker compose up -d
docker compose logs -f fastapi
docker compose exec fastapi pytest tests/ -v
curl http://localhost/api/health
```

### Docker (VPS)
```bash
bash scripts/deploy.sh
docker compose logs -f
curl https://your-domain.com/api/health
```
