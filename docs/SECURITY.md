# LOSE10 — Security & Privacy Model

## 1. Authentication
- Registration/login via email + password (Argon2id or bcrypt hashing, never reversible encryption).
- Access tokens: short-lived JWT (e.g. 15 min). Refresh tokens: opaque, hashed at rest, rotating with family-based reuse detection (a reused/revoked refresh token invalidates the entire family and forces re-login — signals possible token theft).
- Email verification required before full account activation (WhatsApp connection gated on verified email + active subscription).
- Password reset via single-use, time-limited, hashed token sent by email; endpoint always returns 200 regardless of whether the email exists (no user enumeration).
- Account deletion: soft-delete + scheduled purge per retention policy (see §8).

## 2. Authorization
- Roles: `USER, ADMIN, SUPPORT, COACH`. Enforced server-side via Spring Security method/URL security — frontend role checks are UX only, never trusted.
- Every admin action additionally checked against explicit permissions (not just role membership) where the action is sensitive (e.g. only certain roles can resolve CRITICAL safety events).
- `COACH` role (human coach override) can view conversations and send messages only while `coaching_states.mode = HUMAN_ACTIVE` for that user, or in read-only mode otherwise.

## 3. Multi-Tenancy / Data Isolation
- Every user-scoped query is parameterized by the authenticated `userId` taken from the security context — never from a client-supplied path/body parameter without an ownership check.
- Repository methods for user-owned entities are written as `findByIdAndUserId(...)`, not `findById(...)` — a bare `findById` on a user-owned table is treated as a code-review defect.
- AI tool calls always bind `userId` server-side (see AI_ARCHITECTURE.md §4) — the model can never supply or override it.
- Automated tests (mandatory, see TESTING.md) prove User A cannot read/write User B's profile, memories, messages, weight, or subscription via API or AI tool paths.

## 4. Prompt Injection & AI-Specific Security
- User content is always treated as untrusted data, never concatenated into system instructions (see AI_ARCHITECTURE.md §6).
- Tool outputs are structured data, not natural-language instructions, to block second-order injection via tool results.
- Response validation blocks system-prompt/tool-schema leakage before send (see AI_ARCHITECTURE.md §5).

## 5. Webhook Security
- WhatsApp: HMAC-SHA256 signature verification on every inbound webhook (`X-Hub-Signature-256`), raw-body verification before parsing.
- Stripe: signature verification via `Stripe-Signature` and the webhook signing secret, raw-body verification before parsing.
- Both webhook handlers are idempotent via a persisted event-id table (see DATABASE.md), so at-least-once delivery from either provider never double-processes.

## 6. Rate Limiting (Redis-backed, per class)
| Class | Examples | Approach |
|---|---|---|
| Public/unauthenticated | login, register, password reset | Strict per-IP + per-identifier limits, exponential backoff on repeated failures. |
| User | general API | Per-user token bucket, generous for normal use. |
| AI endpoints | any path that triggers a Claude call | Tighter per-user cap, since this is the direct cost driver — also protects against runaway conversational loops. |
| Webhook | WhatsApp/Stripe | Not rate-limited the same way (trusted signed sources) but still bounded against replay abuse via idempotency, and monitored for anomalous volume. |
| Admin | admin endpoints | Per-admin-account limits + full audit logging (no anonymous/service-account admin access). |

## 7. Audit Logging
Logged (actor, action, target, metadata, timestamp — no sensitive payload bodies in cleartext): `ADMIN_LOGIN, USER_DATA_VIEWED, USER_DATA_EXPORTED, USER_DELETED, MEMORY_DELETED, AI_PAUSED, AI_RESUMED, SUBSCRIPTION_CHANGED, SAFETY_FLAG_CREATED`, plus all `admin_actions`. Audit logs are append-only and excluded from the standard data-export/deletion flow for a legally-required retention window (documented, not assumed — subject to legal review per §9).

## 8. Privacy & Data Rights
Users can, in-app: access their data, export it (`GET /api/v1/users/me/export`), delete memories individually, disconnect WhatsApp, revoke consents, and delete their account. Architecture supports GDPR/CCPA-style rights (access, portability, erasure, consent withdrawal) — **this is an architectural readiness statement, not a legal compliance claim**; actual compliance requires legal review before launch (product spec §51).

- **Consent tracking**: `user_consents` records type (ToS, Privacy Policy, marketing, WhatsApp comms, AI coaching, data processing), version, timestamp, source — versioned so a ToS update doesn't silently apply retroactively.
- **Deletion cascade**: account deletion purges/anonymizes profile, goals, weight, training, nutrition, conversations, memories (structured + embeddings), and subscription PII, retaining only what's legally required (billing records, audit logs) in anonymized/minimal form.
- **Data export**: async job compiles profile, goals, weight, workouts, nutrition, conversations, memories, subscriptions, and consents into a downloadable package; never includes internal secrets, other users' data, or raw system-prompt content.

## 9. Application Security Hardening
- HTTPS enforced in production (TLS termination at the load balancer/reverse proxy); HSTS enabled.
- Secure, `HttpOnly`, `SameSite` cookies where cookies are used; tokens otherwise in memory/secure storage on the client, never `localStorage` for refresh tokens.
- CSRF protection on state-changing cookie-authenticated endpoints (N/A where pure bearer-token API auth is used, but the web app's session-adjacent flows are protected).
- CORS restricted to known frontend origins.
- Input validation (Bean Validation) on every DTO; parameterized queries only (JPA/Hibernate) — no string-concatenated SQL anywhere, including admin tooling.
- Output encoding on the frontend (React default-escapes; explicit review for any `dangerouslySetInnerHTML` use, which should not exist for user-generated content).
- Secrets management: no secrets in source control; `.env` files gitignored; production secrets from a managed secret store (cloud provider's secrets manager), rotated periodically.
- Dependency scanning in CI (see DEPLOYMENT.md).

## 10. Safety Escalation
CRITICAL safety events (see AI_ARCHITECTURE.md §7) create a `safety_events` row and trigger an on-call/admin notification path in addition to the scripted safe response to the user. This is a defensive coaching safeguard, not a medical emergency service — user-facing copy for critical situations should direct to appropriate professional/emergency resources rather than the app attempting to handle them, and this copy requires review before launch.

## 11. Human Override Safety
When `coaching_states.mode = HUMAN_ACTIVE`, the AI is prevented from sending automatic responses at the orchestrator's entry gate (checked before any generation call is made, not just before sending) — this avoids a race where AI and human both respond to the same message.
