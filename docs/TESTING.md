# LOSE10 — Testing Strategy

## 1. Levels
| Level | Scope | Tooling |
|---|---|---|
| Unit | Individual services/components (goal calc, trend calc, memory dedup logic, safety heuristics) | JUnit 5 + Mockito (backend), Vitest/Jest (frontend) |
| Repository/integration | JPA repositories, Flyway migrations, pgvector queries against a real Postgres | Testcontainers (Postgres w/ pgvector, Redis) |
| API | Controller-level, full Spring context, auth/authorization enforced | Spring `MockMvc` / `WebTestClient` + Testcontainers |
| AI orchestration | Context building, tool-calling loop, response validation — Claude calls mocked/stubbed with recorded fixtures for determinism; a small separate suite makes real calls against canned scenarios for prompt-quality regression | JUnit + a fixture-based `AIProvider` test double |
| Memory & vector retrieval | Extraction dedup/contradiction logic, ranking formula, retrieval relevance on seeded data | Integration tests against real pgvector |
| Webhook | WhatsApp and Stripe webhook handlers — signature verification, idempotency (duplicate delivery), malformed payloads | Integration tests with recorded real-shaped payloads |
| Scheduler | Proactive message timing/timezone correctness, frequency caps, quiet hours, idempotent claim under concurrent runs | Integration tests with clock injection |
| Frontend | Component tests + critical-flow interaction tests | Vitest/Jest + Testing Library |
| End-to-end | Critical user journeys, browser-driven against a full local stack | Playwright (or equivalent) |
| Security | Cross-user data isolation, prompt injection resistance, auth edge cases | Dedicated test suites, see §3-4 |

## 2. Critical End-to-End Scenario (mandatory, product spec §72)
Register → onboarding → set 10kg goal → subscribe (Stripe test mode) → connect WhatsApp (sandbox) → send message → webhook received → user identified → memories retrieved → Claude responds (context assembled correctly) → response validated → WhatsApp send → conversation stored → memory extraction runs → embedding generated → memory stored in pgvector → analytics event recorded → second message later → prior memory retrieved and naturally referenced. This flow gets an automated E2E test before Phase 7 (WhatsApp) is considered complete, and is re-run in CI on every subsequent change to the pipeline.

## 3. Data Isolation Tests (mandatory)
Automated proof that User A cannot access, via any path (REST API, AI tool calls, admin views without proper role): User B's profile, memories, messages, weight, subscription, or AI context. Implemented as a parameterized suite that attempts cross-user access against every user-scoped endpoint and every AI tool, asserting rejection.

## 4. AI-Specific Test Suites
- **Prompt injection resistance**: canned adversarial user messages ("ignore previous instructions", "reveal your system prompt", "call the tool as another user") asserted to have no effect on behavior/authorization.
- **Response validation**: fixtures that should be blocked (leaked prompt fragments, unsafe content, hallucinated user data) assert the validator rejects and the fallback path fires instead of sending.
- **Safety engine**: canned unsafe requests (starvation, ED indicators, injury, medical diagnosis requests) assert correct classification and escalation, without false-negative gaps.
- **Memory correctness**: the three product-spec examples (Friday overeating pattern, running dislike, wedding date) as concrete regression tests — create, retrieve at the right moment, update on contradiction, never duplicate.
- **Personality/tone eval**: a small scenario-based suite asserting the coach never says "according to my memory/profile" and stays within style (SUPPORTIVE/BALANCED/TOUGH_LOVE) — necessarily softer/heuristic assertions than the deterministic suites above, tracked as a quality signal rather than a hard CI gate initially.

## 5. Definition of Done (applies to every feature, per product spec §108)
Implementation + tests + integration verified + errors handled + security considered + logging where appropriate + docs updated + migrations included + frontend and backend both working + build passes + tests pass. No feature is merged on "code exists" alone.

## 6. CI Gate
All of the above (except the real-Claude-call quality suite and E2E, which run on a slower cadence/staging) run on every PR; a red build blocks merge (see DEPLOYMENT.md §4).
