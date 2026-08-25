# Epic L10-E10: Security & Privacy Hardening

**Maps to:** ROADMAP.md Phase 10
**Goal:** Full security review, privacy rights implementation, complete observability in staging, load testing, and a stabilized E2E suite — the gate before production launch.
**Exit criteria:** SECURITY.md checklist verified in staging; load test results documented; no open CRITICAL findings.

---

### L10-1001 — Full cross-user data isolation suite (all domains)
`Priority: P0` · `Estimate: 8` · `Depends on: L10-211, all domain epics E03-E09` · `Component: platform`

Extends the E02 isolation suite to every user-scoped endpoint and every AI tool across the entire system (SECURITY.md §3).

**Acceptance Criteria**
- Parameterized suite attempts cross-user access against every user-scoped endpoint and every AI tool; all rejected.
- Runs in CI as a standing regression gate, not a one-time manual check.

---

### L10-1002 — Prompt injection & AI security suite (full)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-411, E05, E06` · `Component: ai`

Expands L10-411 to cover memory/tool-output-based second-order injection, response validation bypass attempts, safety-engine evasion attempts.

**Acceptance Criteria**
- No adversarial fixture in the expanded suite alters authorization, leaks internals, or bypasses safety escalation.

---

### L10-1003 — Data export implementation (`GET /users/me/export`)
`Priority: P0` · `Estimate: 5` · `Depends on: all domain epics` · `Component: platform`

Async job compiling profile, goals, weight, workouts, nutrition, conversations, memories, subscriptions, consents into a downloadable package (S3 presigned URL, time-limited).

**Acceptance Criteria**
- Export contains no internal secrets, no other users' data.
- `USER_DATA_EXPORTED` audit-logged.

---

### L10-1004 — Full account deletion cascade
`Priority: P0` · `Estimate: 5` · `Depends on: L10-210, all domain epics` · `Component: platform`

Completes the soft-delete from E02 into a real cascade: purge/anonymize profile, goals, weight, training, nutrition, conversations, memories (structured + embeddings), subscription PII per retention policy.

**Acceptance Criteria**
- Post-deletion, no query path returns the deleted user's personal data (verified by test), except legally-required minimal billing/audit records, which are anonymized.

---

### L10-1005 — Memory deletion audit + cascade correctness
`Priority: P0` · `Estimate: 2` · `Depends on: L10-501` · `Component: memory`

Verifies `DELETE /memories/{id}` removes structured row + embedding + writes `memory_events(DELETED)` + `audit_logs`.

**Acceptance Criteria**
- Hard delete confirmed (not soft) for user-initiated memory deletion, per MEMORY.md §8.

---

### L10-1006 — Consent management UI (`/privacy`, `/terms`, in-app consent controls)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-303` · `Component: frontend`

**Acceptance Criteria**
- Users can view/revoke each consent type in-app.
- Legal pages clearly marked as pending legal review (product spec §91) — not presented as finalized legal text.

---

### L10-1007 — Rate limiting tuning across all endpoint classes
`Priority: P0` · `Estimate: 3` · `Depends on: all API epics` · `Component: platform`

Verifies/tunes the per-class limits from SECURITY.md §6 (public, user, AI, webhook, admin) against realistic traffic assumptions.

**Acceptance Criteria**
- Load test confirms limits actually engage under simulated abuse without impacting normal usage patterns.

---

### L10-1008 — Full observability wiring in staging
`Priority: P0` · `Estimate: 5` · `Depends on: L10-108, AWS staging infra` · `Component: platform, infra`

CloudWatch metrics/alarms, error monitoring service integration, AI cost observability dashboard live against real staging traffic.

**Acceptance Criteria**
- Alarms fire correctly when deliberately-induced staging failures occur (5xx spike, AI error rate, webhook failure — each tested once).

---

### L10-1009 — Secrets management & rotation (AWS Secrets Manager)
`Priority: P0` · `Estimate: 3` · `Depends on: AWS staging infra` · `Component: infra`

**Acceptance Criteria**
- No secret present in any environment file or container image; all pulled from Secrets Manager at runtime.
- DB credential rotation tested at least once without downtime.

---

### L10-1010 — Load testing against expected launch volume
`Priority: P0` · `Estimate: 5` · `Depends on: L10-1008` · `Component: platform`

**Acceptance Criteria**
- Documented results against a defined target (concurrent users, messages/sec, AI call latency under load) with no unbounded error-rate growth.
- Bottlenecks found are either fixed or explicitly accepted with rationale before launch.

---

### L10-1011 — Full E2E suite stabilization
`Priority: P0` · `Estimate: 5` · `Depends on: L10-713, all domain epics` · `Component: platform`

Stabilizes and expands the critical scenario suite (TESTING.md §2) plus additional key journeys (cancel/resubscribe, human override, safety escalation) into a reliable CI/staging gate.

**Acceptance Criteria**
- Suite passes reliably (no flaky failures) across 5 consecutive runs before sign-off.
