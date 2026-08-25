# LOSE10

AI personal weight-loss coach delivering personalized daily coaching, accountability, and planning primarily through WhatsApp. Subscription SaaS, initial program: help an adult lose ~10kg.

**Status:** Epics 1–3 implemented (Foundation, Auth, Profile/Goals/Weight/Onboarding — see [docs/backlog/](docs/backlog/README.md)). See [docs/](docs/) for the full architecture package.

## Running locally
```bash
cp .env.example .env
docker compose up --build
```
Backend: http://localhost:8080/api/v1 · Frontend: http://localhost:5173 · Postgres/Redis run as Compose services.

For backend-only development: `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local` (requires Postgres+pgvector and Redis running — `docker compose up postgres redis`). Requires JDK 21 and Maven locally (this environment didn't have either installed, so the backend has not been compiled here — verify with `mvn clean verify` on a machine with the JDK before deploying).

Frontend-only: `cd frontend && npm install && npm run dev` (verified: builds and type-checks cleanly).

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
backend/   Java 21 + Spring Boot API — auth, profile, goal, weight, progress modules implemented
frontend/  React + TypeScript + Tailwind web app — auth, onboarding, dashboard implemented
infra/     Reserved for Terraform/CDK (Phase 11)
docs/      Architecture, planning documentation, and backlog
```
