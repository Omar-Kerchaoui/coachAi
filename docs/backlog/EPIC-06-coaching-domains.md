# Epic L10-E06: Coaching Domains (Training, Nutrition, Check-ins, Coaching State)

**Maps to:** ROADMAP.md Phase 6
**Goal:** Real training and nutrition systems, check-ins, and coaching state (style + AI/HUMAN/PAUSED), all reachable by the AI through tools.
**Exit criteria:** The AI can read/update plans, log workouts/meals, and run check-ins through tools, fully authorized and tested.

---

### L10-601 — `training_plans`/`training_plan_versions`/`workouts`/`workout_exercises`/`exercise_library` schema
`Priority: P0` · `Estimate: 5` · `Depends on: L10-304` · `Component: training`

**Acceptance Criteria**
- Migration applies cleanly; `exercise_library` seed data loaded (a reasonable starter set across HOME/GYM/OUTDOOR, varied muscle groups/equipment).

---

### L10-602 — `workout_sessions` schema + logging API
`Priority: P0` · `Estimate: 3` · `Depends on: L10-601` · `Component: training`

`POST /training/sessions`, `GET /training/sessions`.

**Acceptance Criteria**
- Logged session links to the workout it was performed against; perceived effort captured.

---

### L10-603 — Training plan generation & adaptation logic
`Priority: P0` · `Estimate: 8` · `Depends on: L10-601` · `Component: training`

Adapts based on goal, fitness level, equipment/environment (HOME/GYM/OUTDOOR), time, fatigue signals, adherence, progress — versioned (new `training_plan_versions` row with `reason_for_change` on adaptation, not a silent overwrite).

**Acceptance Criteria**
- Given a documented adherence-drop scenario, plan adaptation reduces load/frequency rather than continuing to escalate (matches product-spec Day 60 example intent).
- Every adaptation is versioned and auditable.

---

### L10-604 — Training plan API (`GET /training/plan`, `/plan/history`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-603` · `Component: training`

**Acceptance Criteria**
- Returns today's workout specifically, not the whole plan (matches ContextBuilder's compact-context need).

---

### L10-605 — `nutrition_profiles`/`nutrition_logs`/`meals`/`meal_items` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: nutrition`

**Acceptance Criteria**
- Migration applies cleanly.

---

### L10-606 — Nutrition profile API (`GET/PATCH /nutrition/profile`)
`Priority: P0` · `Estimate: 2` · `Depends on: L10-605` · `Component: nutrition`

Dietary restrictions, disliked foods, calorie/protein targets.

**Acceptance Criteria**
- Restrictions/dislikes are structured enough for the AI to reliably avoid recommending them (e.g. "user dislikes running" analog for food — cross-checked against the memory PREFERENCE category from E05, not duplicated logic).

---

### L10-607 — Conversational meal logging (`POST /nutrition/logs/{date}/meals` + AI tool)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-606, L10-408` · `Component: nutrition, ai`

Free-text meal entry with AI calorie/protein estimation — no food database required for v1 (product spec §26).

**Acceptance Criteria**
- "I had eggs and toast" produces a `meal_items` row with a reasonable AI-estimated calorie value and `source = AI_ESTIMATE`.
- Web equivalent (non-WhatsApp) logging also works via the REST endpoint.

---

### L10-608 — `check_ins`/`daily_check_ins`/`weekly_reviews` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: checkin`

**Acceptance Criteria**
- Migration applies cleanly.

---

### L10-609 — Check-in completion API + `createCheckIn` AI tool
`Priority: P0` · `Estimate: 5` · `Depends on: L10-608, L10-408` · `Component: checkin, ai`

**Acceptance Criteria**
- Check-in responses stored structured (mood/energy/adherence), retrievable by `getRecentCheckIns` tool.

---

### L10-610 — Weekly review generation (AI-authored summary)
`Priority: P1` · `Estimate: 5` · `Depends on: L10-609, L10-410` · `Component: checkin, ai`

Generated via the orchestrator (reuses safety/validation pipeline), not a raw unvalidated prompt call.

**Acceptance Criteria**
- Weekly review references real progress data (weight trend, adherence) pulled from actual tables, not hallucinated.

---

### L10-611 — `coaching_states`/`coaching_events`/`coach_preferences` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: coaching`

**Acceptance Criteria**
- Migration applies cleanly; one `coaching_states` row per user, `mode` defaults to `AI_ACTIVE`.

---

### L10-612 — Coaching state API (style change, pause/resume)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-611` · `Component: coaching`

User-facing controls from product spec §31 (change coaching style, pause/resume coaching).

**Acceptance Criteria**
- Setting `PAUSED` immediately halts both reactive AI responses and proactive scheduling (cross-checked once E07 lands, but the state/gate itself is testable now against the orchestrator entry check).

---

### L10-613 — Remaining write tools: `logWeight`/`logWorkout`/`logMeal`/`updateTrainingPlan`
`Priority: P0` · `Estimate: 5` · `Depends on: L10-602, L10-607, L10-603, L10-408` · `Component: ai`

Applies the shared tool-security decorator (ownership, validation, business rules, audit log) from AI_ARCHITECTURE.md §4.

**Acceptance Criteria**
- `logWeight` tool reuses the same implausible-delta validation as the REST endpoint (L10-307) — no duplicated logic.
- `updateTrainingPlan` respects `coaching_states.mode` restrictions.

---

### L10-614 — Full `ContextBuilder` completion (training/nutrition/check-in sections)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-604, L10-606, L10-609, L10-407` · `Component: ai`

Removes the remaining stubs from E04's `ContextBuilder`, respecting token budget caps.

**Acceptance Criteria**
- Full context block matches the MEMORY.md/AI_ARCHITECTURE.md §3 structure end-to-end, with no stubbed sections remaining.
