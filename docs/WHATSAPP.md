# LOSE10 — WhatsApp Architecture

WhatsApp is the primary coaching channel. All access goes through a `WhatsAppProvider` interface — no other module references the Meta SDK/HTTP client directly.

```java
interface WhatsAppProvider {
    SendResult sendText(String waId, String text);
    SendResult sendTemplate(String waId, String templateName, Map<String,Object> params);
    void verifyWebhookSignature(String payload, String signatureHeader);
}
```

## 1. Inbound Flow
```
POST /api/v1/whatsapp/webhook
  1. Verify X-Hub-Signature-256 (HMAC against app secret) — reject if invalid.
  2. Extract Meta's event id; check whatsapp_webhook_events for existing row (idempotency).
     If already processed → 200 OK, no-op.
  3. Persist whatsapp_webhook_events row (raw payload) before any processing.
  4. Resolve identity: wa_id/phone → whatsapp_accounts → user. Unknown number → log, 200 OK, no further action.
  5. Check account/subscription/coaching state (opted out? subscription inactive? PAUSED?) — apply gating rules.
  6. Hand off to conversation pipeline (see ARCHITECTURE.md §5).
  7. Mark webhook event processed.
```
Return `200 OK` quickly (Meta retries on non-2xx / timeout) — heavy processing (AI generation) happens synchronously within the request today at expected v1 volume, but the design tolerates moving generation to an async worker later without changing the webhook contract, since the webhook's only durable responsibility is steps 1-4.

## 2. Outbound Flow
- Coach responses and proactive messages both go through the same `WhatsAppProvider.sendText/sendTemplate`.
- Every outbound send persisted to `messages` + `whatsapp_messages` with the returned `wa_message_id`, before considering the send complete.
- Delivery/read status updates arrive as separate webhook events, matched by `wa_message_id`, updating `whatsapp_messages.status`.
- Retries: transient send failures retried with backoff (bounded attempts); permanent failures (e.g. number opted out) marked `FAILED` and surfaced to analytics, not silently swallowed.

## 3. Opt-in / Opt-out
- Connecting WhatsApp (`POST /api/v1/whatsapp/connect`) sends an explicit opt-in template message; `whatsapp_accounts.status` moves `PENDING → CONNECTED` only after the user responds/confirms per Meta's business messaging policy.
- Any inbound "STOP"/opt-out-style message (or explicit disconnect in-app) sets `status = OPTED_OUT` and halts all outbound messaging (proactive and reactive) immediately — checked at the top of the send path, not just the scheduler.

## 4. Proactive Coaching Scheduler

Scheduled message types: `MORNING_CHECKIN, PRE_WORKOUT, POST_WORKOUT, EVENING_CHECKIN, WEIGHT_REMINDER, MISSED_WORKOUT, MISSED_CHECKIN, WEEKLY_REVIEW, RE_ENGAGEMENT, MILESTONE`.

```
Scheduler (periodic job, e.g. every 5-15 min, Redis lock to prevent double-run across instances)
  1. Query due candidates: users whose local time matches a configured trigger
     (timezone-aware — computed from user_profiles.timezone, not server time).
  2. For each candidate, apply gating:
       - notification_preferences (quiet hours, per-type enabled/disabled)
       - coaching_states.mode != PAUSED
       - whatsapp_accounts.status == CONNECTED
       - frequency cap (max N proactive messages / day, configurable) — never spam
       - relevant context available (e.g. don't send MISSED_WORKOUT if there was no scheduled workout)
  3. Insert/claim a scheduled_messages row with idempotency key
     (user_id, message_type, scheduled_for::date) — prevents duplicate sends on retry/overlap.
  4. Compose message: for simple reminders, templated; for behavior-aware nudges
     (e.g. Friday pattern), routed through AIOrchestrator with a synthetic "proactive" intent
     so it benefits from the same context/safety/validation pipeline as reactive replies.
  5. Send via WhatsAppProvider, mark scheduled_messages SENT, record notification_events + analytics.
```

### Re-engagement rule (per product spec §86)
Inactive users are not immediately spammed. Example cadence: Day 1 silent, Day 2 a light check-in, later a low-pressure "let's just get today right" nudge — implemented as a `RE_ENGAGEMENT` message-type with its own backoff schedule, still subject to notification preferences and frequency caps.

## 5. Templates vs. Free-form
- Meta requires pre-approved templates for messages sent outside the 24-hour customer-service window. `MORNING_CHECKIN`, `RE_ENGAGEMENT`, and other cold-open proactive types use approved templates; replies within an active session use free-form text.
- Template catalog and approval status tracked in config, not hardcoded per call site, so template changes don't require a code deploy.

## 6. Media Handling (architecture, not full v1 scope)
- Inbound media (e.g. a meal photo) is downloaded via Meta's media API, stored in object storage (not the DB), and a reference (URL/key) attached to the relevant domain row (e.g. `meal_items.media_ref`). Processing (e.g. AI image description) is a pluggable step in the conversation pipeline, added when the product scope calls for it — the schema/architecture accommodates it without redesign.

## 7. Cost Tracking
Every outbound send (template vs. session message, since Meta prices them differently) and inbound message is attributable to a user for the WhatsApp cost dashboard (see BUSINESS_LOGIC.md §WhatsApp Cost).
