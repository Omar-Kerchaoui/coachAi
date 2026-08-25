# Epic L10-E07: WhatsApp Integration

**Maps to:** ROADMAP.md Phase 7
**Goal:** WhatsApp becomes the live coaching channel — inbound/outbound messaging, proactive scheduling, fully wired to the AI orchestrator.
**Exit criteria:** The full critical E2E scenario (TESTING.md §2) passes against WhatsApp sandbox.

---

### L10-701 — `whatsapp_accounts`/`whatsapp_messages`/`whatsapp_webhook_events` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: whatsapp`

**Acceptance Criteria**
- Migration applies cleanly; unique index on `whatsapp_webhook_events.event_id` and `whatsapp_messages.wa_message_id`.

---

### L10-702 — `WhatsAppProvider` interface + Cloud API implementation
`Priority: P0` · `Estimate: 5` · `Depends on: L10-701` · `Component: whatsapp`

`sendText`, `sendTemplate`, `verifyWebhookSignature`; no other module references the Meta SDK/HTTP client directly.

**Acceptance Criteria**
- Architecture test asserts only the `whatsapp` package references Meta APIs.
- Signature verification unit-tested against known-good and tampered payloads.

---

### L10-703 — Webhook verification handshake (`GET /whatsapp/webhook`)
`Priority: P0` · `Estimate: 1` · `Depends on: L10-702` · `Component: whatsapp`

**Acceptance Criteria**
- Handshake succeeds against Meta's verification challenge format.

---

### L10-704 — Inbound webhook handler: signature + idempotency + identity resolution
`Priority: P0` · `Estimate: 5` · `Depends on: L10-702` · `Component: whatsapp`

Per WHATSAPP.md §1 steps 1-5: verify signature, dedupe via `whatsapp_webhook_events`, resolve phone→user, gate on subscription/opt-out/paused state.

**Acceptance Criteria**
- Duplicate delivery of the same Meta event id is a no-op on the second delivery (explicit test).
- Unknown phone number logged and 200'd without further action.
- Invalid signature rejected (no processing).

---

### L10-705 — Wire webhook handler to `AIOrchestrator`
`Priority: P0` · `Estimate: 3` · `Depends on: L10-704, L10-410` · `Component: whatsapp, ai`

Completes ARCHITECTURE.md §5's pipeline end-to-end for real inbound messages.

**Acceptance Criteria**
- A real (sandbox) inbound message produces a validated AI response.

---

### L10-706 — Outbound send + persistence
`Priority: P0` · `Estimate: 3` · `Depends on: L10-705` · `Component: whatsapp`

Every send persisted to `messages` + `whatsapp_messages` with `wa_message_id` before considered complete; retry with bounded backoff on transient failure.

**Acceptance Criteria**
- Send failure after exhausting retries marks the message `FAILED`, not silently dropped.

---

### L10-707 — Delivery/read status webhook handling
`Priority: P1` · `Estimate: 2` · `Depends on: L10-706` · `Component: whatsapp`

Updates `whatsapp_messages.status` from status-update webhook events, matched by `wa_message_id`.

**Acceptance Criteria**
- Status transitions (sent→delivered→read) reflected correctly for a sandbox test message.

---

### L10-708 — Opt-in / opt-out flow
`Priority: P0` · `Estimate: 3` · `Depends on: L10-706` · `Component: whatsapp`

`POST /whatsapp/connect` sends opt-in template; inbound "STOP"-style messages or explicit disconnect set `OPTED_OUT` and halt all outbound messaging immediately.

**Acceptance Criteria**
- After opt-out, both reactive and proactive sends are blocked at the send-path entry point (not just the scheduler).

---

### L10-709 — `notification_preferences`/`scheduled_messages`/`notification_events` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-701` · `Component: notification`

**Acceptance Criteria**
- Migration applies cleanly; index on `(status, scheduled_for)` for the scheduler's poll query.

---

### L10-710 — Proactive scheduler core (timezone-aware, Redis-locked, idempotent)
`Priority: P0` · `Estimate: 8` · `Depends on: L10-709` · `Component: notification`

Periodic job selecting due candidates by user local time; Redis distributed lock prevents double-run across instances; idempotency key `(user_id, message_type, scheduled_for::date)`.

**Acceptance Criteria**
- Two concurrent scheduler runs (simulated) never send the same scheduled message twice.
- A user in a different timezone than the server fires at the correct local time (explicit test with clock/timezone injection).

---

### L10-711 — Scheduled message types + gating (frequency caps, quiet hours, coaching state)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-710, L10-612` · `Component: notification`

All ten message types (`MORNING_CHECKIN` ... `MILESTONE`); gating per WHATSAPP.md §4 step 2.

**Acceptance Criteria**
- `PAUSED` coaching state or `OPTED_OUT`/disconnected WhatsApp blocks every scheduled type.
- Frequency cap enforced (test: many due candidates in one day, only the configured max sent).

---

### L10-712 — Behavior-aware proactive messages (routed through orchestrator)
`Priority: P1` · `Estimate: 5` · `Depends on: L10-711, L10-512` · `Component: notification, ai`

E.g. Friday-pattern nudges — composed via `AIOrchestrator` with a synthetic "proactive" intent so they get the same safety/validation pipeline as reactive replies.

**Acceptance Criteria**
- A seeded Friday-overeating pattern produces a contextually relevant proactive nudge, validated same as any AI response.

---

### L10-713 — Critical E2E scenario test (WhatsApp sandbox)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-705, L10-706, L10-711` · `Component: whatsapp, platform`

Automates the full TESTING.md §2 scenario against the WhatsApp sandbox environment.

**Acceptance Criteria**
- Full scenario (register → onboarding → subscribe stub → connect WhatsApp → message → memory created → later message references it) passes end-to-end in CI/staging (subscription step can stub `ACTIVE` status until E08 lands if sequenced in parallel — swap to real Stripe-gated state once E08 is done).
