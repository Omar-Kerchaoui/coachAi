# LOSE10 — Development Roadmap

Each phase produces working, tested, documented software per the Definition of Done (TESTING.md §5) — no phase is "done" on code existing alone. Phases are sequential dependencies, not strict calendar sprints; scope within a phase can be re-sequenced but later phases assume earlier ones are functionally complete.

Each phase below is tracked as an Epic in the ticket-level backlog: see [docs/backlog/](backlog/README.md) for the full breakdown into 128 stories with acceptance criteria, estimates, and dependencies.

## Phase 1 — Foundation
Repository structure, Docker Compose (Postgres+pgvector, Redis, backend, frontend), Spring Boot skeleton with base config/error handling/logging, React skeleton with routing shell, Flyway baseline, CI pipeline (build+test+lint for both apps), `.env.example`.
**Exit criteria:** `docker compose up` gives a running, empty-but-wired stack; CI green on an empty-feature PR.

## Phase 2 — Authentication & Identity
`users`, `user_roles`, `sessions`, `refresh_tokens`; registration, login, logout, refresh rotation, email verification, password reset; Spring Security role enforcement; rate limiting on auth endpoints.
**Exit criteria:** full auth API tested, including cross-user isolation tests for authenticated resources.

## Phase 3 — Profile, Goals, Weight
`user_profiles/preferences/settings/consents`, `goals/goal_milestones/goal_events`, `weight_entries`, onboarding flow (API + frontend), trend calculation (rolling average, no single-reading overreaction).
**Exit criteria:** a user can complete onboarding, set a 10kg goal, log weight, and see a correct trend on the dashboard.

## Phase 4 — AI Foundation
`AIProvider`/`EmbeddingProvider` abstractions, `ClaudeAIProvider` implementation, `AIOrchestrator` skeleton (IntentDetector, SafetyAnalyzer, ContextBuilder, ResponseValidator — memory retrieval stubbed until Phase 5), initial ToolRegistry (read-only tools first), `ai_requests` cost logging wired from the first call.
**Exit criteria:** a text-in/validated-text-out flow works end-to-end via a test harness (no WhatsApp yet), with every call logged for cost.

## Phase 5 — Memory & Behavior
`memories/memory_events/memory_embeddings` (pgvector + HNSW index), MemoryExtractor async pipeline, MemoryRetriever (structured + semantic + ranking), conversation summarization, `behavior_patterns/behavior_events` engine.
**Exit criteria:** the three product-spec memory scenarios (Friday overeating, running dislike, wedding date) pass as automated tests; retrieved memories visibly and naturally influence AI responses in the test harness.

## Phase 6 — Coaching Domains
Training (`training_plans` and children, exercise library, workout sessions), Nutrition (profile, logs, conversational meal logging), Check-ins (daily/weekly), `coaching_states` (style + AI/HUMAN/PAUSED), remaining write tools in ToolRegistry.
**Exit criteria:** the AI can read/update plans, log workouts/meals, and run check-ins through tools, fully authorized and tested.

## Phase 7 — WhatsApp
`WhatsAppProvider`, webhook verification + idempotency, inbound/outbound message flow wired to the full conversation pipeline, opt-in/opt-out, proactive scheduler (all message types, timezone-aware, frequency-capped), template catalog.
**Exit criteria:** the full critical E2E scenario (TESTING.md §2) passes against WhatsApp sandbox.

## Phase 8 — Stripe & Billing
`plans/coupons/subscriptions/subscription_events/payments/refunds`, Checkout + Customer Portal integration, webhook handling (idempotent, authoritative), trial logic, access gating tied to subscription status.
**Exit criteria:** register→trial→paid→cancel→resubscribe all work against Stripe test mode with webhook-driven state, verified by tests that simulate webhook delivery (including out-of-order/duplicate delivery).

## Phase 9 — Dashboard, Admin & Analytics
User dashboard (progress, plan, coach status), admin console (user detail view, human override, safety review queue, notes), `analytics_events` catalog wired across the app, business metrics rollups, AI cost dashboard, WhatsApp cost dashboard.
**Exit criteria:** an admin can view any user's full state, pause/resume AI, resolve a safety event, and see accurate cost/business metrics reflecting seeded test data.

## Phase 10 — Hardening
Security review pass (data isolation suite, prompt injection suite, rate limiting tuning), privacy features (data export, account deletion cascade, consent management UI), full observability (structured logs, metrics, error monitoring wired in staging), load testing against expected launch volume, full E2E suite stabilized.
**Exit criteria:** SECURITY.md checklist verified in staging; load test results documented; no open CRITICAL findings.

## Phase 11 — Production Launch
Production infra provisioned per DEPLOYMENT.md, monitoring/alerting live, automated backups verified with a real restore test, DNS/TLS/cert live, legal pages reviewed by counsel (ToS/Privacy/Health Disclaimer/AI Disclaimer — explicitly flagged as requiring human legal review, not auto-approved), staged rollout.
**Exit criteria:** production smoke test of the full critical scenario with live (not sandbox) WhatsApp/Stripe on a small cohort before general availability.

## Explicitly Deferred (not blocking launch)
Additional languages beyond English, additional programs beyond Lose10 (maintenance mode's *domain hooks* exist from Phase 3, but its full experience is v2), a dedicated vector database (only if pgvector scale limits are actually hit), a message broker/microservices split (only if the modular monolith's scheduler/job-table approach actually bottlenecks).
