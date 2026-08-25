# Epic L10-E08: Stripe & Billing

**Maps to:** ROADMAP.md Phase 8
**Goal:** Full subscription lifecycle on Stripe, webhook-authoritative, gating coaching access correctly.
**Exit criteria:** Register→trial→paid→cancel→resubscribe all work against Stripe test mode with webhook-driven state, including out-of-order/duplicate delivery.

---

### L10-801 — `plans`/`coupons`/`subscriptions`/`subscription_events`/`payments`/`refunds` schema
`Priority: P0` · `Estimate: 5` · `Depends on: L10-201` · `Component: subscription`

**Acceptance Criteria**
- Migration applies cleanly; unique indexes on `stripe_subscription_id`, `stripe_event_id`, `stripe_payment_intent_id`, `stripe_refund_id` (idempotency).

---

### L10-802 — Stripe product/price setup ($19.99/mo + annual option)
`Priority: P0` · `Estimate: 2` · `Depends on: L10-801` · `Component: subscription`

Configure Stripe Products/Prices (test mode), seed `plans` table with `stripe_price_id` references.

**Acceptance Criteria**
- Monthly and annual plans both exist and are selectable; pricing is data-driven (`plans` table), not hardcoded in application logic.

---

### L10-803 — Checkout session creation (`POST /subscriptions/checkout-session`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-802` · `Component: subscription`

Server-side Stripe Checkout Session creation — no client-constructed payment forms.

**Acceptance Criteria**
- Session includes trial period per plan config.
- Coupon code applies correctly when provided.

---

### L10-804 — Stripe webhook handler (signature verification + idempotency)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-801` · `Component: subscription`

Raw-body signature verification via `Stripe-Signature`; dedupe via `subscription_events.stripe_event_id`.

**Acceptance Criteria**
- Invalid signature rejected without processing.
- Same event id delivered twice processed exactly once (explicit test).

---

### L10-805 — Handle subscription lifecycle events
`Priority: P0` · `Estimate: 5` · `Depends on: L10-804` · `Component: subscription`

`customer.subscription.created/updated/deleted`, `customer.subscription.trial_will_end`.

**Acceptance Criteria**
- `subscriptions.status` always mirrors Stripe's state after processing — never set optimistically from a client redirect (explicit test: client "success" redirect without a corresponding webhook does NOT activate the subscription).

---

### L10-806 — Handle payment/invoice events
`Priority: P0` · `Estimate: 5` · `Depends on: L10-804` · `Component: subscription`

`invoice.paid`, `invoice.payment_failed`, `charge.refunded`.

**Acceptance Criteria**
- Failed payment moves subscription toward `past_due`/`canceled` per Stripe's own status progression (relying on Stripe Smart Retries, not reimplementing dunning logic).
- Refund event creates a `refunds` row linked to the correct `payments` row.

---

### L10-807 — Customer Portal integration (`POST /subscriptions/portal-session`)
`Priority: P0` · `Estimate: 2` · `Depends on: L10-803` · `Component: subscription`

Self-service plan change/cancellation/payment method update.

**Acceptance Criteria**
- Portal session correctly scoped to the requesting user's Stripe customer (ownership-checked).

---

### L10-808 — Subscription status API (`GET /subscriptions/me`, `POST /subscriptions/cancel`)
`Priority: P0` · `Estimate: 2` · `Depends on: L10-805` · `Component: subscription`

**Acceptance Criteria**
- Cancel sets `cancel_at_period_end`, does not immediately cut access (matches Stripe default behavior and product spec's non-manipulative cancellation requirement).

---

### L10-809 — Access gating: subscription status → coaching access
`Priority: P0` · `Estimate: 3` · `Depends on: L10-805, L10-704` · `Component: subscription, whatsapp`

Lapsed/past_due/canceled subscription pauses WhatsApp coaching access (not silently continuing service).

**Acceptance Criteria**
- Inbound message from a user with an inactive subscription gets a clear, non-generation-triggering response (no wasted AI cost on a non-paying, lapsed user) rather than full coaching.

---

### L10-810 — Billing UI (pricing page, checkout flow, billing settings page)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-803, L10-807, L10-808` · `Component: frontend`

`/pricing`, checkout redirect flow, `/billing` page showing current plan/status/portal link.

**Acceptance Criteria**
- Full flow (pricing → checkout → return → dashboard reflects active subscription only after webhook confirms it) works against Stripe test mode.
