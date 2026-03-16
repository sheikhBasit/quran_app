# Development Guide

## TDD Workflow — The Only Way We Write Code

```
RED   → Write the failing test first
GREEN → Write minimum code to make it pass  
REFACTOR → Clean up — tests must still pass
```

**Never write production code without a failing test first.**

## Test Commands

```bash
# Mobile — fast unit tests (no device needed)
./gradlew :composeApp:testDebugUnitTest

# Mobile — UI tests (needs connected device/emulator)  
./gradlew :composeApp:connectedAndroidTest

# Backend
cd backend && pytest --cov=app -v
```

## Branch Strategy

```
main          ← production, protected
develop       ← integration branch
feature/xxx   ← one branch per phase/feature
```

## Commit Message Format

```
feat(phase-4): add AyahCard component with RTL support
test(phase-4): add AyahCard compose UI tests
fix(phase-2): correct SQLDelight tafsir query ordering
chore: update Kotlin to 2.0.21
```

## Before Every Commit

```bash
# Both must pass
./gradlew :composeApp:testDebugUnitTest && cd backend && pytest
```

## Phase Completion Checklist

Before marking a phase complete:
- [ ] All tests written and passing
- [ ] No compiler warnings
- [ ] Tested on real Android device (not just emulator)
- [ ] CI pipeline green on GitHub Actions
- [ ] Exit criteria in MVP plan checked off

## Adding a New Feature

1. Create branch: `git checkout -b feature/phase-X-feature-name`
2. Open Antigravity — paste session start template
3. Use phase prompt from `docs/antigravity/antigravity-prompts.md`
4. Follow TDD — test first, always
5. Push and open PR to `develop`
6. CI must pass before merge

## Custom Skills Location

All Antigravity skills are in `.skills/`:

| Skill | When it applies |
|---|---|
| `tdd-guardian` | Every feature — enforces TDD |
| `kmp-architecture` | Any Kotlin file |
| `quran-arabic-ui` | Any Quran Composable |
| `rag-pipeline` | Any backend RAG work |
| `docker-deploy` | Docker/VPS/deployment |
| `islamic-data` | Data audit/ingestion |

**Register in Antigravity:** Settings → Custom Skill Paths → `.skills`
