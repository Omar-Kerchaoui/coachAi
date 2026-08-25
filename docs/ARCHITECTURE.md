# LOSE10 — System Architecture

## 1. Product Summary
LOSE10 is a subscription SaaS AI weight-loss coach. Customers interact primarily through WhatsApp; a React web app handles registration, onboarding, billing, and progress visualization. The system's core differentiator is **persistent, structured customer memory** — the coach gets measurably better at coaching each individual the longer they use it.

The first program is fixed: **help an adult lose ~10kg**, through daily conversational coaching, structured plans (training + nutrition), check-ins, and proactive outreach. The domain model is built so future programs (maintenance, muscle gain, etc.) can be added without a rewrite, but only one program ships now.

## 2. Technology Decisions

| Layer | Choice | Why |
|---|---|---|
| Backend | Java 21 + Spring Boot | Mature, typed, strong ecosystem for scheduled jobs, security, transactional integrity — appropriate for a system where billing and health-adjacent data correctness matter. |
| API layer | Spring MVC REST, versioned (`/api/v1`) | Simple, cacheable, testable. WebSocket reserved for a future "live typing" admin console feature only — not required for v1. |
| ORM | Spring Data JPA + Hibernate | Standard, works well with Flyway-managed schema. |
| Migrations | Flyway | Deterministic, auditable schema history; mandatory for a schema this large. |
| Primary DB | PostgreSQL 16 | Relational integrity for billing/health data; JSONB for flexible metadata; native extension support for pgvector. |
| Vector search | pgvector (in the same Postgres instance) | Avoids a second data store for memory v1; transactional consistency between structured memory rows and their embeddings; simplest possible operational footprint at launch scale. Revisit a dedicated vector DB only if scale (>10M vectors, high QPS) demands it. |
| Cache / jobs / locks | Redis | Rate limiting, idempotency keys, distributed locks for the scheduler, short-lived session state. Not a system of record. |
| Frontend | React + TypeScript + Tailwind | Standard, fast to iterate, strong typing reduces integration bugs against the API. |
| AI | Claude API, behind an `AIProvider` abstraction | Best-in-class instruction following and tool use for a coaching persona; abstraction protects against vendor lock-in. |
| Messaging | WhatsApp Cloud API, behind a `WhatsAppProvider` abstraction | Primary UX channel per product spec. |
| Payments | Stripe | Industry standard subscription billing; webhooks are the source of truth. |
| Infra | Docker Compose (local) → containerized cloud deployment | Reproducible local dev; portable to any container platform. |
| Background jobs | Spring `@Scheduled` + a durable job table (outbox-style) for anything that must survive a restart, backed by Redis locks for coordination | Avoids introducing a heavyweight queue (Kafka/SQS) before it's needed; the job table gives retryability/idempotency without new infra. Revisit if job volume outgrows a single-instance scheduler. |
| Observability | SLF4J + structured JSON logs, Micrometer metrics, Spring Actuator health/readiness, request-ID propagation | Enough to run this safely in production without building a full observability platform on day one. |

**Explicit non-decisions (deferred, not ignored):** no Kafka/message broker, no separate vector database, no microservices split. The system is a modular monolith with clean internal domain boundaries (see §4) so any of these can be extracted later if scale requires it. Introducing them now would be premature for a pre-revenue product.

## 3. High-Level Architecture

```
                    ┌─────────────────────┐
                    │      INTERNET       │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │ React Web Platform  │  (marketing, auth, dashboard, admin)
                    └──────────┬──────────┘
                               │ HTTPS / REST (/api/v1)
                    ┌──────────▼──────────┐
                    │   Spring Boot API   │
                    │ ─────────────────── │
                    │ Auth · User · Goal  │
                    │ Coaching · Training │
                    │ Nutrition · Progress│
                    │ Subscription · Admin│
                    │ WhatsApp Webhooks   │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼───────────────────────┐
        │                      │                        │
┌───────▼────────┐   ┌─────────▼────────┐    ┌──────────▼─────────┐
│  PostgreSQL     │   │      Redis        │    │  pgvector (in PG)  │
│  business data  │   │ cache/locks/rate  │    │  memory embeddings │
│  source of truth│   │ limit/idempotency │    │                    │
└─────────────────┘   └───────────────────┘    └─────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   AI Orchestrator    │
                    │ IntentDetector        │
                    │ SafetyAnalyzer        │
                    │ ContextBuilder        │
                    │ MemoryRetriever       │
                    │ ToolRegistry          │
                    │ ResponseValidator     │
                    │ MemoryExtractor       │
                    └──────────┬──────────┘
                               │
                       ┌───────▼───────┐
                       │  Claude API    │
                       └───────────────┘

       ┌────────────────┐          ┌────────────────┐
       │ WhatsApp Cloud  │          │ Stripe Billing  │
       │ API             │          │                 │
       └────────────────┘          └────────────────┘

                    ┌────────────────────┐
                    │ Background Workers  │
                    │ memory extraction   │
                    │ embeddings          │
                    │ summarization       │
                    │ proactive scheduler │
                    │ analytics rollups   │
                    └────────────────────┘
```

## 4. Internal Module Boundaries (Modular Monolith)

Single deployable backend, organized as package-per-domain with explicit interfaces between domains (no domain reaches into another domain's repositories directly — it goes through a service interface). This is a DDD-inspired boundary, not full hexagonal ceremony.

```
com.lose10
 ├─ auth            (users, roles, sessions, tokens)
 ├─ profile          (user_profiles, preferences, settings, consents)
 ├─ goal             (goals, milestones, goal events)
 ├─ weight           (weight_entries, body_measurements, trend calc)
 ├─ progress         (snapshots, metrics, milestone detection)
 ├─ training         (plans, workouts, exercises, sessions)
 ├─ nutrition        (nutrition profile, logs, meals)
 ├─ checkin          (daily check-ins, weekly reviews)
 ├─ coaching         (coaching state, style, human override)
 ├─ conversation     (threads, messages, summaries)
 ├─ memory           (memories, embeddings, extraction, retrieval)
 ├─ behavior         (pattern detection engine)
 ├─ ai               (AIProvider, EmbeddingProvider, orchestrator, tools, safety, validation)
 ├─ whatsapp         (WhatsAppProvider, webhook handling, delivery)
 ├─ notification     (scheduler, notification prefs, scheduled_messages)
 ├─ subscription     (Stripe integration, plans, coupons, payments)
 ├─ admin            (admin views, human intervention, notes)
 ├─ safety           (safety events, flags, review workflow)
 ├─ analytics        (event tracking, business metrics, AI/WhatsApp cost dashboards)
 └─ platform         (shared: error handling, rate limiting, audit log, config)
```

Rule: `ai`, `whatsapp`, `subscription` are the only modules allowed to talk to external systems (Claude, Meta, Stripe respectively), always behind an interface (`AIProvider`, `WhatsAppProvider`, no direct Stripe SDK calls outside `subscription`).

## 5. Conversation Processing Pipeline (mandatory shape)

```
Inbound WhatsApp message
  → Webhook signature verification + idempotency (event_id)
  → Identity resolution (phone → user)
  → Auth/consent check (opted in? subscription active? paused?)
  → Persist raw inbound message
  → Safety pre-screen (cheap heuristic pass; full AI safety pass happens in orchestrator)
  → AIOrchestrator.handle(userId, message)
      → IntentDetector            (what is this about: log weight, ask question, vent, etc.)
      → SafetyAnalyzer            (block/escalate dangerous requests before generation)
      → ContextBuilder            (assembles compact structured context, see AI_ARCHITECTURE.md)
      → MemoryRetriever           (structured + pgvector semantic search)
      → ToolRegistry              (exposes user-scoped tools to Claude)
      → ClaudeProvider.generate() (tool-calling loop)
      → ResponseValidator         (safety/format/hallucination checks)
  → Send via WhatsAppProvider
  → Persist outbound message
  → Enqueue async: MemoryExtractor, embeddings, conversation summarization, analytics event
```

This pipeline is mandatory conceptually; implementation may batch or parallelize steps, but no step may be skipped and no step may be replaced by a raw "prompt in, response out" call.

## 6. Data Ownership

- **PostgreSQL** is the source of truth for all structured business data: identity, billing, goals, weight, training, nutrition, conversations, memory *records*, behavior patterns, notifications, analytics.
- **pgvector** stores *only* the embedding vectors tied 1:1 to a `memories` row (plus model/version metadata) — never raw business state.
- **Redis** is disposable: cache, rate-limit counters, distributed locks, idempotency keys, short-lived session artifacts. Nothing here is the only copy of anything important.

## 7. Related Documents
- [DATABASE.md](DATABASE.md) — ERD and full schema
- [API.md](API.md) — REST API specification
- [AI_ARCHITECTURE.md](AI_ARCHITECTURE.md) — orchestrator, tools, safety, providers
- [MEMORY.md](MEMORY.md) — memory system and vector architecture
- [WHATSAPP.md](WHATSAPP.md) — WhatsApp integration and proactive scheduler
- [SECURITY.md](SECURITY.md) — authn/authz, data isolation, hardening
- [DEPLOYMENT.md](DEPLOYMENT.md) — infra, CI/CD, observability
- [BUSINESS_LOGIC.md](BUSINESS_LOGIC.md) — Stripe, cost model, business metrics
- [TESTING.md](TESTING.md) — testing strategy
- [ROADMAP.md](ROADMAP.md) — phased delivery plan
