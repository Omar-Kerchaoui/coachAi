# Epic L10-E04: AI Foundation

**Maps to:** ROADMAP.md Phase 4
**Goal:** The AI provider abstraction, orchestration pipeline skeleton, and cost logging exist and work end-to-end via a test harness (no WhatsApp yet).
**Exit criteria:** A text-in/validated-text-out flow works end-to-end via a test harness, with every call logged for cost.

---

### L10-401 — `AIProvider` / `EmbeddingProvider` interfaces
`Priority: P0` · `Estimate: 3` · `Depends on: L10-102` · `Component: ai`

Define the abstraction per AI_ARCHITECTURE.md §1; no other module may reference the Claude SDK directly once this lands (enforced via architecture test / module boundary check).

**Acceptance Criteria**
- Interfaces compile with clear contracts for generate/continue/embed/batch-embed.
- An architecture test (e.g. ArchUnit) asserts only the `ai` package imports the Claude SDK.

---

### L10-402 — `ClaudeAIProvider` implementation
`Priority: P0` · `Estimate: 5` · `Depends on: L10-401` · `Component: ai`

Wraps Claude API calls: timeout, retry with backoff (idempotent only), circuit breaker (fail closed).

**Acceptance Criteria**
- Timeout and retry behavior covered by tests using a fake/mock transport.
- Circuit breaker trip results in a safe fallback, never an unvalidated pass-through.

---

### L10-403 — `ai_requests` schema + mandatory cost logging wrapper
`Priority: P0` · `Estimate: 3` · `Depends on: L10-402` · `Component: ai, analytics`

Every Claude call logs user, provider, model, request_type, input/output tokens, estimated cost, latency — enforced at the wrapper level, not per call site.

**Acceptance Criteria**
- A test asserts calling `AIProvider.generate()` always produces exactly one `ai_requests` row, even on error paths.
- Cost estimate calculation documented and unit-tested against known token counts.

---

### L10-404 — `EmbeddingProvider` implementation
`Priority: P0` · `Estimate: 3` · `Depends on: L10-401` · `Component: ai`

Single + batch embedding generation; records model/dimension/version/token usage per call.

**Acceptance Criteria**
- Batch call reduces per-item overhead vs. N single calls (verified by call-count assertions in tests).

---

### L10-405 — `IntentDetector`
`Priority: P0` · `Estimate: 5` · `Depends on: L10-402` · `Component: ai`

Classifies inbound messages: `LOG_DATA / QUESTION / VENTING / SMALL_TALK / SAFETY_CONCERN / COMMAND`.

**Acceptance Criteria**
- Test fixture set of representative messages classified correctly (used as a regression suite going forward).
- Uses a cheaper/faster model path where appropriate (cost control, AI_ARCHITECTURE.md §9).

---

### L10-406 — `SafetyAnalyzer` (pre-generation screen)
`Priority: P0` · `Estimate: 8` · `Depends on: L10-402` · `Component: ai, safety`

Detects starvation/dehydration/eating-disorder/injury/medical-diagnosis/unsafe-training requests; short-circuits generation on HIGH/CRITICAL.

**Acceptance Criteria**
- Canned unsafe-request fixtures (per TESTING.md §4) correctly classified with no false negatives in the test set.
- `safety_events` row created on detection (schema lands with this story if not already present).

---

### L10-407 — `ContextBuilder` skeleton
`Priority: P0` · `Estimate: 5` · `Depends on: L10-402` · `Component: ai`

Assembles the compact context block from available data at this phase (profile, goal, weight/trend — memory/behavior sections stubbed until E05, training/nutrition sections stubbed until E06); enforces per-section token budget caps.

**Acceptance Criteria**
- Token budget caps are configuration, not hardcoded magic numbers scattered through code.
- Oversized sections are truncated/summarized, never silently sent unbounded.

---

### L10-408 — `ToolRegistry` skeleton + first read-only tools
`Priority: P0` · `Estimate: 5` · `Depends on: L10-407` · `Component: ai`

`getUserProfile`, `getGoal`, `getWeightHistory` as the first tools, with the ownership/validation decorator from AI_ARCHITECTURE.md §4 applied uniformly.

**Acceptance Criteria**
- Tool calls always bind `userId` from the authenticated server-side context, never from model-supplied arguments (explicit test: a forged `userId` argument is ignored/rejected).
- Every tool call logged.

---

### L10-409 — `ResponseValidator`
`Priority: P0` · `Estimate: 5` · `Depends on: L10-402` · `Component: ai`

Length check, leaked-prompt/tool-schema detection, unsafe-content re-check, unvalidated-claim cross-check, medical-diagnosis-language block.

**Acceptance Criteria**
- Fixtures that should be blocked (per TESTING.md §4) are rejected; fallback response used instead of sending.
- A failed validation is logged with enough context to review why.

---

### L10-410 — `AIOrchestrator` composition (skeleton pipeline)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-405, L10-406, L10-407, L10-408, L10-409` · `Component: ai`

Wires IntentDetector → SafetyAnalyzer → ContextBuilder → ToolRegistry → ClaudeAIProvider → ResponseValidator into the single orchestrator entry point (memory retrieval joins in E05).

**Acceptance Criteria**
- End-to-end test: text message in → validated text response out, via a test harness (not yet WhatsApp).
- Each pipeline stage is independently unit-testable (orchestrator itself only composes, no embedded business logic).

---

### L10-411 — Prompt injection resistance test suite (foundation)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-410` · `Component: ai`

First installment of the adversarial suite from TESTING.md §4 ("ignore previous instructions", "reveal your system prompt", forged tool arguments).

**Acceptance Criteria**
- Suite runs in CI against the orchestrator skeleton.
- No adversarial fixture alters authorization or leaks system instructions.
