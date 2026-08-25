# LOSE10 — Billing, Business Metrics & Cost Model

## 1. Subscription Model
- Single initial plan: **$19.99 USD/month**, with an annual option and a free trial, managed entirely as Stripe `Price`/`Product` objects referenced by `plans.stripe_price_id` — pricing changes are a config/data change, not a code change.
- Multi-currency is a future extension: `plans.currency` and `payments.currency` are modeled from day one even though only USD launches, so adding a currency is additive.
- Coupons/promotional pricing via Stripe Coupons, mirrored in `coupons` for in-app display and eligibility checks.

## 2. Stripe Integration
- **Checkout**: `POST /api/v1/subscriptions/checkout-session` creates a Stripe Checkout Session server-side (never construct payment forms manually — avoids PCI scope entirely).
- **Customer Portal**: `POST /api/v1/subscriptions/portal-session` for self-service plan changes/cancellation/payment method updates.
- **Webhooks are authoritative.** The frontend/API never marks a subscription active based on client-side redirect success — only a verified `checkout.session.completed` / `customer.subscription.updated` webhook updates `subscriptions.status`. This prevents users from spoofing a successful payment client-side.
- Handled webhook events (idempotent via `subscription_events.stripe_event_id`, see DATABASE.md): subscription created/updated/deleted, trial will end, invoice paid, invoice payment failed, charge refunded.
- Failed payments: Stripe's built-in retry (Smart Retries) handles dunning; on final failure, subscription moves to `past_due`/`canceled` per Stripe's status, gating WhatsApp coaching access accordingly (coaching pauses, not silently continues, on a lapsed subscription).
- Refunds processed via Stripe (admin-initiated or Stripe-side), mirrored into `refunds`.

## 3. Analytics Event Catalog
Append-only `analytics_events`: `USER_REGISTERED, ONBOARDING_STARTED, ONBOARDING_COMPLETED, TRIAL_STARTED, SUBSCRIPTION_STARTED, SUBSCRIPTION_CANCELLED, WEIGHT_LOGGED, WORKOUT_COMPLETED, CHECKIN_COMPLETED, AI_MESSAGE_SENT, AI_MESSAGE_RECEIVED, WHATSAPP_MESSAGE_SENT, MEMORY_CREATED, MEMORY_UPDATED, GOAL_MILESTONE_REACHED, USER_REENGAGED, SAFETY_EVENT`. Each event carries `user_id` (nullable for pre-signup), `metadata jsonb`, `occurred_at`. This is the input to all business metrics below — computed from this stream plus domain tables, not duplicated ad hoc per dashboard.

## 4. Business Metrics (admin dashboard)
CAC, LTV, MRR, ARR, churn, retention (day-30, etc.), trial→paid conversion, activation rate, DAU/WAU, messages per user, avg. AI cost/user, avg. WhatsApp cost/user, gross margin, revenue per user. Computed via nightly rollup jobs (not live heavy aggregation queries on request path) into summary tables/materialized views, refreshed for the admin dashboard.

Also surfaced qualitatively (per product spec §84): active vs. stalled vs. at-risk-of-churn user counts, users who reached milestones, per-user cost, trial conversion rate, 30-day retention — derived from the same event stream plus goal/progress state (e.g. "stalled" = no weight-trend improvement + low check-in adherence over a rolling window, a defined heuristic documented alongside its implementation, not hidden logic).

## 5. AI Cost Control
This is treated as a first-class operational concern, not an afterthought — unmanaged AI spend can directly break unit economics on a $19.99/mo product.

- **Every** Claude call is logged to `ai_requests`: user, provider, model, request_type, input/output tokens, estimated cost, latency (see AI_ARCHITECTURE.md §9 — enforced at the provider-wrapper level, not per call site, so it cannot be skipped).
- Cost containment techniques baked into the architecture: compact structured context over raw history dumps (AI_ARCHITECTURE.md §3), conversation summarization instead of ever-growing transcripts, memory retrieval capped top-K (MEMORY.md §6), cheaper models for low-stakes sub-tasks (intent classification) vs. the primary conversational model, and per-user AI rate limiting (SECURITY.md §6) as a hard backstop against runaway usage (bug or abuse).
- **AI cost dashboard** (admin): total spend, spend per user, average cost/user, cost by model, cost by request type, daily/monthly spend, estimated gross margin — all derived from `ai_requests` rollups.
- **WhatsApp cost dashboard**: messages sent/received, template vs. session message counts (priced differently by Meta), estimated cost, cost per active user — derived from `whatsapp_messages`.

### Illustrative unit-economics guardrail (to validate, not a commitment)
At $19.99/mo, AI + WhatsApp variable cost per active user needs to stay low enough to sustain healthy gross margin alongside payment processing fees and infra amortization. The context-budgeting and model-routing decisions above exist specifically so this ratio is controllable via configuration (context size limits, model choice) rather than requiring an architecture change if costs run high — this should be watched closely against real usage data starting Phase 4, and the AI cost dashboard (Phase 9) is scheduled early enough to catch problems before scale amplifies them.

## 6. Data Export (user-facing)
See SECURITY.md §8 — the export bundle draws from goal, weight, training, nutrition, conversation, memory, subscription, and consent tables; produced as an async job (avoids a slow synchronous request) with a time-limited download link, itself an auditable action (`USER_DATA_EXPORTED`).

## 7. Retention Mechanics (non-manipulative)
Milestones, weekly reviews, progress summaries, goal countdowns, and re-engagement messaging (see WHATSAPP.md §4) drive retention through genuine value delivery — explicitly not dark patterns (no fake urgency, no guilt-based manipulation, no disguised unsubscribe flows). Cancellation must be at least as easy as subscription (Stripe Customer Portal handles this by design).

## 8. Post-Goal State
On reaching the target weight, the user's `goals.status` moves to `ACHIEVED` and coaching transitions to a `MAINTENANCE` mode (a configuration/state change on the existing goal engine, not a new product) — subscription continues if the user chooses, coaching shifts tone toward habit maintenance. Full maintenance program design (v2) is out of scope for this document but the goal/coaching state model does not block it.
