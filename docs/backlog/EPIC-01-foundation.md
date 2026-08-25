# Epic L10-E01: Foundation & Repository Setup

**Maps to:** ROADMAP.md Phase 1
**Goal:** A running, empty-but-wired local stack with CI, so every subsequent epic has a foundation to build on.
**Exit criteria:** `docker compose up` gives a running empty-but-wired stack (Postgres+pgvector, Redis, backend, frontend); CI is green on an empty-feature PR.

---

### L10-101 — Initialize monorepo structure and base tooling
`Priority: P0` · `Estimate: 2` · `Depends on: —` · `Component: infra`

Set up the repo layout (`backend/`, `frontend/`, `infra/`, `docs/`), git init, `.gitignore`, `.editorconfig`, root `README.md` (already drafted).

**Acceptance Criteria**
- Repo initialized with the agreed folder layout; `.gitignore` excludes build artifacts, `.env`, IDE files.
- README explains layout and links to `docs/`.

---

### L10-102 — Spring Boot backend skeleton (Java 21, Maven)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-101` · `Component: platform`

Bootstrap the Spring Boot app: Maven build, base package structure per ARCHITECTURE.md §4 module list, Spring profiles (`local`/`staging`/`prod`), base `application.yml`.

**Acceptance Criteria**
- `mvn clean install` succeeds with no source files beyond a health placeholder.
- Package skeleton for all 20 modules exists (empty package-info or marker classes acceptable at this stage).
- Profile-based config loads correctly for `local`.

---

### L10-103 — React frontend skeleton (TypeScript, Tailwind)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-101` · `Component: frontend`

Bootstrap the React app with TypeScript, Tailwind, routing shell for the page list in product spec §55 (empty placeholder pages), base component structure.

**Acceptance Criteria**
- `npm run build` succeeds.
- Routes exist (even as placeholders) for `/`, `/pricing`, `/how-it-works`, `/faq`, `/login`, `/register`, `/onboarding`, `/dashboard`, `/progress`, `/plan`, `/coach`, `/memories`, `/settings`, `/billing`, `/privacy`, `/terms`, `/admin`.
- Tailwind theme configured with a base design token set.

---

### L10-104 — Docker Compose local stack
`Priority: P0` · `Estimate: 3` · `Depends on: L10-102, L10-103` · `Component: infra`

`docker-compose.yml` wiring `postgres` (pgvector-enabled image), `redis`, `backend`, `frontend`.

**Acceptance Criteria**
- `docker compose up` starts all four services healthy.
- Postgres image has `pgvector` extension available (`CREATE EXTENSION IF NOT EXISTS vector;` succeeds).
- Backend connects to Postgres/Redis using compose service DNS names, not hardcoded IPs.

---

### L10-105 — Flyway baseline migration
`Priority: P0` · `Estimate: 2` · `Depends on: L10-104` · `Component: platform`

Wire Flyway into the backend; create `V1__baseline.sql` (empty or minimal marker table) to prove the pipeline works before domain schema lands in later epics.

**Acceptance Criteria**
- Migrations run automatically on backend startup against local Postgres.
- Flyway history table present and correct after startup.

---

### L10-106 — `.env.example` and secrets handling convention
`Priority: P0` · `Estimate: 1` · `Depends on: L10-104` · `Component: infra`

Document all env var categories per DEPLOYMENT.md §1 with placeholder values; confirm `.env` is gitignored.

**Acceptance Criteria**
- `.env.example` covers DATABASE/REDIS/CLAUDE/EMBEDDING/WHATSAPP/STRIPE/AUTH/EMAIL/OBSERVABILITY categories.
- No real secret values anywhere in the repo (verified by a manual scan / secret-scanning tool).

---

### L10-107 — Base error handling & response envelope
`Priority: P0` · `Estimate: 2` · `Depends on: L10-102` · `Component: platform`

Implement the uniform error format from API.md (`code`, `message`, `requestId`, `details`), global exception handler, request-ID generation/propagation.

**Acceptance Criteria**
- Any unhandled exception returns the standard error envelope, never a raw stack trace.
- Every response (success or error) carries a request ID, logged and returned.

---

### L10-108 — Structured logging baseline
`Priority: P0` · `Estimate: 2` · `Depends on: L10-107` · `Component: platform`

JSON structured logging (see DEPLOYMENT.md §5), request-ID in MDC, log level via `OBSERVABILITY_*` config.

**Acceptance Criteria**
- Logs emitted as structured JSON locally.
- Request ID present on every log line within a request's lifecycle.

---

### L10-109 — CI pipeline (GitHub Actions)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-102, L10-103, L10-105` · `Component: infra`

Backend build+test, frontend build+lint+test, Docker image build validation, Flyway migration validation, dependency vulnerability scan — every PR, fails closed.

**Acceptance Criteria**
- PR against an empty-feature branch runs all checks and passes.
- A deliberately broken build (bad test) fails the pipeline and blocks merge (verified once, then reverted).
- Pipeline defined as code in `.github/workflows/`.

---

### L10-110 — Health/readiness endpoints
`Priority: P1` · `Estimate: 1` · `Depends on: L10-104` · `Component: platform`

`/actuator/health` liveness + a readiness check verifying DB/Redis connectivity; restrict sensitive actuator endpoints.

**Acceptance Criteria**
- Liveness returns UP with all services running, DOWN if backend can't start.
- Readiness returns DOWN if DB or Redis is unreachable.
- `/actuator/env`, `/actuator/beans` etc. not publicly exposed.
