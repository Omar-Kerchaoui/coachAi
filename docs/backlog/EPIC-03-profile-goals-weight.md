# Epic L10-E03: Profile, Goals, Weight & Onboarding

**Maps to:** ROADMAP.md Phase 3
**Goal:** A user can complete onboarding, set a 10kg weight-loss goal, log weight, and see a correct trend.
**Exit criteria:** User can complete onboarding, set a 10kg goal, log weight, and see a correct trend on the dashboard.

---

### L10-301 — `user_profiles`/`preferences`/`settings`/`consents` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: profile`

Migrations for all four tables per DATABASE.md, including `timezone`, `locale`, `country` fields (no hardcoded CA/US/UK logic).

**Acceptance Criteria**
- Migration applies cleanly, FK to `users`.
- Timezone stored as IANA tz name, validated on write.

---

### L10-302 — Profile API (`GET/PATCH /users/me`, `/me/preferences`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-301` · `Component: profile`

**Acceptance Criteria**
- PATCH validates each field (height, DOB plausibility, enum values for coach style/units).
- GET returns profile + preferences + settings in one payload per API.md.

---

### L10-303 — Consent recording (`POST /users/me/consents`, `DELETE /consents/{type}`)
`Priority: P0` · `Estimate: 2` · `Depends on: L10-301` · `Component: profile`

Versioned consent tracking (ToS, Privacy Policy, marketing, WhatsApp comms, AI coaching, data processing).

**Acceptance Criteria**
- Consent grant stores type, version, timestamp, source.
- Revoke sets `revoked_at`, never hard-deletes the historical record.

---

### L10-304 — `goals`/`goal_milestones`/`goal_events` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: goal`

**Acceptance Criteria**
- Migration applies cleanly.
- Service-layer constraint: exactly one `ACTIVE` goal per user, enforced (not just DB-level).

---

### L10-305 — Goal engine: create/update goal (`POST/PATCH /goals`)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-304` · `Component: goal`

Starting/current/target weight, target date, remaining-to-lose calculation; edits write `goal_events` audit entries.

**Acceptance Criteria**
- Creating a goal with target 10kg below starting weight succeeds and computes "10kg to go" correctly.
- Editing target/date after creation is logged, not silently overwritten.

---

### L10-306 — `weight_entries`/`body_measurements` schema
`Priority: P0` · `Estimate: 2` · `Depends on: L10-201` · `Component: weight`

**Acceptance Criteria**
- Migration applies cleanly, indexed on `(user_id, recorded_at)`.

---

### L10-307 — Weight logging API (`POST/GET /weight`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-306` · `Component: weight`

**Acceptance Criteria**
- Implausible entries (e.g. >25% delta from last entry) flagged for confirmation, not silently accepted (this rule is shared later with the AI's `logWeight` tool in E06 — implement the validation as a reusable service method).
- Paginated history with date-range filter.

---

### L10-308 — Weight trend calculation (`GET /weight/trend`)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-307` · `Component: weight`

Rolling 7-day average, trend direction — must not overreact to single-day fluctuation (product spec §24).

**Acceptance Criteria**
- Unit tests: a single up-day inside a downward trend does not flip the reported trend direction.
- Trend series returned in a chart-ready format.

---

### L10-309 — `progress_snapshots`/`progress_metrics` schema + nightly computation job
`Priority: P0` · `Estimate: 5` · `Depends on: L10-308, L10-305` · `Component: progress`

**Acceptance Criteria**
- Nightly job computes rolling average, trend, and stores a snapshot per active user.
- Job is idempotent (safe to re-run for the same day).

---

### L10-310 — Progress summary API (`GET /progress/summary`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-309` · `Component: progress`

Current/target/lost/remaining/% progress/phase — powers the dashboard.

**Acceptance Criteria**
- Matches the product-spec example numbers given the same inputs (82→77→72kg scenario).

---

### L10-311 — Onboarding flow (backend + frontend)
`Priority: P0` · `Estimate: 8` · `Depends on: L10-302, L10-305, L10-307` · `Component: profile, frontend`

Progressive-question onboarding UX (product spec §58): current weight → target weight → biggest obstacle → coach style, ending in a goal + profile + preferences all set.

**Acceptance Criteria**
- Completing onboarding creates profile, preferences, and an ACTIVE goal in one coherent flow (not a generic multi-field form).
- Abandoning partway through and resuming later doesn't duplicate records.
- `ONBOARDING_STARTED`/`ONBOARDING_COMPLETED` analytics events fired (stubbed until E09's analytics table lands — see L10-901 dependency note).

---

### L10-312 — Dashboard shell (frontend)
`Priority: P1` · `Estimate: 5` · `Depends on: L10-310` · `Component: frontend`

Current weight, target, lost, remaining, %, today's focus placeholder (real "today's workout" wires in E06), WhatsApp connection status placeholder (wires in E07).

**Acceptance Criteria**
- Dashboard renders real data from `/progress/summary` for a logged-in user.
- No fabricated/placeholder numbers shown once real data exists.
