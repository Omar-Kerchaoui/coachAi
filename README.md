# LOSE10

AI personal weight-loss coach delivering personalized daily coaching, accountability, and planning primarily through WhatsApp. Subscription SaaS, initial program: help an adult lose ~10kg.

**Status:** Epics 1–6 implemented (Foundation, Auth, Profile/Goals/Weight/Onboarding, AI Foundation, Memory/Behavior Engine, Coaching Domains — see [docs/backlog/](docs/backlog/README.md)). See [docs/](docs/) for the full architecture package.

## Running locally
```bash
cp .env.example .env
docker compose up --build
```
Backend: http://localhost:8080/api/v1 · Frontend: http://localhost:5173 · Postgres/Redis run as Compose services.

**To exercise real AI coaching (Epics 4–6)**, set `CLAUDE_API_KEY` and the `EMBEDDING_*` variables in `.env` before starting — without them, the AI orchestrator, memory extraction, and meal-calorie estimation all fail soft (logged, graceful scripted fallback) rather than erroring, which is by design but means you won't see real coaching responses or semantic memory retrieval until a key is supplied.

For backend-only development: `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local` (requires Postgres+pgvector and Redis running — `docker compose up postgres redis`). Requires JDK 21 and Maven.

Frontend-only: `cd frontend && npm install && npm run dev` (verified: builds and type-checks cleanly).

### Testing
`mvn test` runs the pure unit test suite (no Docker required). `mvn verify` additionally runs Testcontainers-backed integration tests against a real Postgres+pgvector — these require a working Docker daemon; on Windows with Docker Desktop, if you see `Could not find a valid Docker environment`, try `export DOCKER_HOST=npipe:////./pipe/docker_engine` first (a known Testcontainers/Docker Desktop compatibility quirk unrelated to this codebase).

## Documentation
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — system architecture, tech stack decisions, module boundaries, conversation pipeline
- [docs/DATABASE.md](docs/DATABASE.md) — ERD and full schema, indexing, pgvector strategy
- [docs/API.md](docs/API.md) — REST API specification
- [docs/AI_ARCHITECTURE.md](docs/AI_ARCHITECTURE.md) — AI orchestrator, provider abstraction, tools, safety, response validation
- [docs/MEMORY.md](docs/MEMORY.md) — memory system, extraction, retrieval, vector architecture
- [docs/WHATSAPP.md](docs/WHATSAPP.md) — WhatsApp integration, proactive coaching scheduler
- [docs/SECURITY.md](docs/SECURITY.md) — auth, authorization, data isolation, privacy, hardening
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — infra, CI/CD, observability
- [docs/BUSINESS_LOGIC.md](docs/BUSINESS_LOGIC.md) — Stripe billing, business metrics, AI/WhatsApp cost model
- [docs/TESTING.md](docs/TESTING.md) — testing strategy
- [docs/ROADMAP.md](docs/ROADMAP.md) — phased delivery plan (Phase 1–11)
- [docs/backlog/](docs/backlog/README.md) — Jira-style backlog: 128 stories across 11 epics, one per roadmap phase

## Repository Layout
```
backend/   Java 21 + Spring Boot API — auth, profile, goal, weight, progress, ai, memory,
           behavior, training, nutrition, checkin, coaching modules implemented
frontend/  React + TypeScript + Tailwind web app — auth, onboarding, dashboard implemented
infra/     Reserved for Terraform/CDK (Phase 11)
docs/      Architecture, planning documentation, and backlog
```

## Not yet wired up
- **WhatsApp (Epic 7)** — coaching is currently reachable only via an internal test endpoint (`POST /api/v1/_internal/coach/message`), not a real channel.
- **Live AI verification** — no `CLAUDE_API_KEY`/`EMBEDDING_*` credentials have been available in this environment; the AI orchestration, memory extraction, and meal estimation code paths are unit-tested with fakes and verified to fail gracefully without a key, but real generated coaching text and semantic retrieval quality haven't been observed against the live Anthropic/Voyage APIs.
