# Epic L10-E09: Dashboard, Admin & Analytics

**Maps to:** ROADMAP.md Phase 9
**Goal:** A complete admin application and business/AI/WhatsApp cost visibility, plus the analytics event backbone used throughout the product.
**Exit criteria:** An admin can view any user's full state, pause/resume AI, resolve a safety event, and see accurate cost/business metrics reflecting seeded test data.

---

### L10-901 — `analytics_events` schema + event-emission helper
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: analytics`

Central `AnalyticsRecorder` service; wires in the events already referenced as stubs in earlier epics (`USER_REGISTERED`, `ONBOARDING_STARTED/COMPLETED`, `TRIAL_STARTED`, `SUBSCRIPTION_STARTED/CANCELLED`, `WEIGHT_LOGGED`, `WORKOUT_COMPLETED`, `CHECKIN_COMPLETED`, `AI_MESSAGE_SENT/RECEIVED`, `WHATSAPP_MESSAGE_SENT`, `MEMORY_CREATED/UPDATED`, `GOAL_MILESTONE_REACHED`, `USER_REENGAGED`, `SAFETY_EVENT`) per BUSINESS_LOGIC.md §3.

**Acceptance Criteria**
- Every listed event type is actually emitted at its corresponding real code path (audit each one against E02-E08).
- Recording an event never blocks/fails the triggering request (fire-and-forget or async, with its own error handling).

---

### L10-902 — `safety_events`/`safety_flags`/`safety_reviews` schema (if not already landed in E04)
`Priority: P0` · `Estimate: 2` · `Depends on: L10-406` · `Component: safety`

**Acceptance Criteria**
- Migration applies cleanly if not already present from L10-406; reviewer workflow fields present.

---

### L10-903 — `admin_notes`/`admin_actions`/`human_interventions` schema
`Priority: P0` · `Estimate: 2` · `Depends on: L10-201` · `Component: admin`

**Acceptance Criteria**
- Migration applies cleanly.

---

### L10-904 — Admin auth & authorization (SUPPORT/ADMIN/COACH scoping)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-209` · `Component: admin`

Backend authorization for every `/admin/*` endpoint, per-action permission checks beyond role membership for sensitive actions (e.g. CRITICAL safety resolution).

**Acceptance Criteria**
- Non-admin gets 403 on all `/admin/*` routes (isolation test extended from L10-211).

---

### L10-905 — Admin user list/search (`GET /admin/users`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-904` · `Component: admin`

**Acceptance Criteria**
- Search by email/phone/name; paginated.

---

### L10-906 — Admin user detail view (`GET /admin/users/{id}`)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-905` · `Component: admin`

Full state: profile, goal, weight history, progress, workouts, meals, check-ins, memories, behavior patterns, conversations, safety events, subscription, AI usage — per product spec §43.

**Acceptance Criteria**
- Every section renders real cross-domain data for a seeded test user.
- Access is `USER_DATA_VIEWED` audit-logged.

---

### L10-907 — Human coach override (coaching-state control + manual send)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-906, L10-612` · `Component: admin, coaching`

`POST /admin/users/{id}/coaching-state`, `POST /admin/conversations/{threadId}/messages`.

**Acceptance Criteria**
- Setting `HUMAN_ACTIVE` reliably prevents the AI from auto-responding to the next inbound message (verified against the orchestrator's entry gate from SECURITY.md §11).
- Manual admin message sent only while `HUMAN_ACTIVE` (or an explicit allowed exception), audit-logged.

---

### L10-908 — Admin notes (`POST /admin/users/{id}/notes`)
`Priority: P1` · `Estimate: 1` · `Depends on: L10-906` · `Component: admin`

---

### L10-909 — Safety review queue (`GET /admin/safety-events`, `POST .../resolve`)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-902, L10-904` · `Component: admin, safety`

CRITICAL events additionally trigger an on-call/admin notification (SECURITY.md §10).

**Acceptance Criteria**
- CRITICAL event creation fires a notification (email/Slack/etc. — mechanism decided at implementation, but must be real, not just a DB row).
- Resolution recorded with reviewer and resolution notes.

---

### L10-910 — Business metrics rollup jobs + `GET /admin/metrics/business`
`Priority: P0` · `Estimate: 8` · `Depends on: L10-901` · `Component: analytics`

CAC, LTV, MRR, ARR, churn, retention, trial conversion, activation rate, DAU/WAU, messages/user, active/stalled/at-risk counts, milestone counts.

**Acceptance Criteria**
- Nightly rollup, not a live heavy-aggregation query on the dashboard request path.
- "Stalled" heuristic documented alongside its implementation (per BUSINESS_LOGIC.md §4) and unit-tested against a constructed scenario.

---

### L10-911 — AI cost dashboard (`GET /admin/metrics/ai-cost`)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-403, L10-901` · `Component: analytics`

Total spend, spend/user, avg cost/user, cost by model, cost by request type, daily/monthly spend, estimated gross margin.

**Acceptance Criteria**
- Numbers reconcile against raw `ai_requests` rows for a seeded dataset (exact match, no drift from the rollup logic).

---

### L10-912 — WhatsApp cost dashboard (`GET /admin/metrics/whatsapp-cost`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-706, L10-901` · `Component: analytics`

Messages sent/received, template vs. session counts, estimated cost, cost per active user.

**Acceptance Criteria**
- Template vs. session message costing reflects Meta's actual differentiated pricing model structure.

---

### L10-913 — Admin dashboard frontend
`Priority: P0` · `Estimate: 8` · `Depends on: L10-905, L10-906, L10-909, L10-910, L10-911, L10-912` · `Component: frontend`

`/admin` — users, active/trial/subscriber counts, MRR/churn/retention, AI/WhatsApp cost, revenue/margin, engagement, user detail drill-down, safety queue, intervention controls.

**Acceptance Criteria**
- All admin capabilities from product spec §43-44 reachable through the UI, not just the API.

---

### L10-914 — User-facing progress visualization (`/progress` page)
`Priority: P1` · `Estimate: 5` · `Depends on: L10-310, L10-309` · `Component: frontend`

Weight graph, trend line, starting point, target, milestones, adherence, workout completion — understated language, no exaggerated fluctuation display (product spec §57).

**Acceptance Criteria**
- Single-day fluctuations visually de-emphasized relative to the trend line (matches the "don't overreact" rule from weight trend logic).
