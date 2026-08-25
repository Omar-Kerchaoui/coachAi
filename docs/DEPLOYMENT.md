# LOSE10 — Deployment, CI/CD & Observability

## 1. Local Development
Docker Compose services: `postgres` (with pgvector extension enabled at init), `redis`, `backend`, `frontend`. Flyway migrations run automatically on backend startup against the local Postgres. A `docker-compose.yml` plus `.env.example` (never committed with real secrets) ships in Phase 1.

Environment variable categories (`.env.example` documents all, values blank/placeholder):
```
DATABASE_*        (url, user, password, pool size)
REDIS_*           (url)
CLAUDE_*          (api key, model id)
EMBEDDING_*       (provider, api key, model id, dimension)
WHATSAPP_*        (phone number id, access token, app secret, verify token)
STRIPE_*          (secret key, publishable key, webhook signing secret, price ids)
AUTH_*            (jwt signing secret, token TTLs)
EMAIL_*           (SMTP/provider credentials)
OBSERVABILITY_*   (log level, metrics endpoint, error monitoring DSN)
```

## 2. Production Deployment Architecture — AWS

Cloud provider is **AWS**. Target service mapping (confirmed for Phase 1/11 implementation):

```
Route 53 (DNS) → CloudFront (CDN + TLS via ACM) → S3 (frontend static build)
                                                  → ALB (TLS via ACM) → ECS Fargate: backend API service (≥2 tasks, autoscaling)
                                                                       → ECS Fargate: worker service (scheduler, memory extraction,
                                                                                        embeddings, summarization, analytics rollups)
Backend/worker tasks reach:
   ├─ RDS for PostgreSQL 16 (pgvector extension enabled) — Multi-AZ, automated backups + PITR
   ├─ ElastiCache for Redis — cache/locks/rate-limit/idempotency
   ├─ S3 — media (WhatsApp inbound media, data export bundles), private bucket, presigned URLs only
   ├─ Secrets Manager — DB creds, Claude API key, WhatsApp token, Stripe keys, JWT signing secret
   ├─ SES — transactional email (verification, password reset, billing notices)
   └─ CloudWatch — logs, metrics, alarms; X-Ray optional for tracing if needed later
```

- **Compute**: ECS Fargate (not EKS) — no Kubernetes operational overhead needed for a modular monolith at launch scale; container images built in CI, pushed to **ECR**, deployed via ECS service update. Revisit EKS only if the team later needs Kubernetes-specific tooling.
- **Backend is stateless** (sessions/tokens externalized to RDS/ElastiCache) so ECS can run multiple tasks behind the ALB and autoscale on CPU/request count.
- **Workers run as a separate ECS service** (same image, worker entrypoint/profile) so a slow background job never starves API-serving capacity; scales independently.
- **Networking**: VPC with public subnets (ALB, NAT) and private subnets (ECS tasks, RDS, ElastiCache) — DB and cache are never internet-reachable. Security groups scoped per tier (ALB→ECS, ECS→RDS/ElastiCache only).
- **Secrets**: nothing in source control or plain environment files in production; ECS task definitions pull from Secrets Manager at runtime. Rotation configured for DB credentials at minimum.
- **IaC**: infrastructure defined as code (Terraform or AWS CDK — decided at Phase 1 kickoff, CDK is a reasonable default given the Java/TypeScript stack) rather than hand-clicked in the console, so environments are reproducible and reviewable.

## 3. Backups & Disaster Recovery
- RDS automated backups with point-in-time recovery enabled, Multi-AZ for failover; this is the system of record and must never be single-copy.
- Backup restore tested periodically against a scratch RDS instance (documented in RUNBOOK.md at implementation time), not just configured and forgotten.
- ElastiCache Redis is treated as disposable/rebuildable — no backup requirement, since nothing is solely stored there (see ARCHITECTURE.md §6).
- S3 buckets versioned; lifecycle policies applied to export bundles (auto-expire presigned exports after a short window).

## 4. CI/CD
Every pull request runs (fails closed — broken code does not merge):
- Backend build + unit/integration tests (Maven, against an ephemeral Postgres+pgvector + Redis in CI, e.g. via Testcontainers)
- Frontend build + lint + unit tests
- Static analysis / dependency vulnerability scan
- Docker image build validation for both backend and frontend
- Flyway migration validation (migrations apply cleanly to a fresh schema)

Deployment: merge to main → GitHub Actions builds tagged images → push to ECR → deploy to staging ECS automatically → manual promotion to production ECS (no automatic prod deploy without a gate, given this handles billing and health-adjacent data).

## 5. Observability
- **Logging**: structured JSON logs shipped to CloudWatch Logs, request-ID propagated through the whole pipeline (webhook → orchestrator → tool calls → send), correlatable end-to-end for a single conversation turn.
- **Metrics** (Micrometer → CloudWatch): request latency/error rates, AI call latency/error rate, AI token usage, WhatsApp send success/failure, Stripe webhook processing latency, scheduler job success/failure, DB pool saturation; CloudWatch Alarms on the critical ones (5xx rate, AI error rate, webhook failures, DB connection saturation).
- **Health checks**: `/actuator/health` (liveness) wired to the ALB target group health check, a readiness variant checking DB/Redis connectivity — not publicly exposing sensitive actuator endpoints (env, beans, etc. restricted to internal network/admin auth via security group, not internet-facing).
- **Error monitoring**: exceptions captured to an error-tracking service (e.g. Sentry-class tool) with request-ID correlation, PII-scrubbed.
- **AI cost observability**: real-time-ish via `ai_requests` (see DATABASE.md), surfaced in the admin dashboard (see BUSINESS_LOGIC.md).

## 6. Environments
`local` (Docker Compose) → `staging` (mirrors prod, used for QA and Stripe/WhatsApp sandbox testing) → `production`. Staging uses Stripe test mode and WhatsApp's test number; production uses live credentials, never shared.
