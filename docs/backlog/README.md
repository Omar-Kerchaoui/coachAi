# LOSE10 — Product Backlog

Jira-style backlog derived from [../ROADMAP.md](../ROADMAP.md). Each roadmap phase is an **Epic**; each epic is broken into **Stories** with acceptance criteria, estimate, priority, and dependencies, ready to import into a real tracker (Jira/Linear/GitHub Projects).

## Conventions

- **Ticket ID**: `L10-<epic><seq>` — e.g. `L10-101` = Epic 1, story 01. Epic number matches the roadmap phase number (Epic 1 = Phase 1 Foundation, ... Epic 11 = Phase 11 Production Launch).
- **Priority**: `P0` blocks the epic's exit criteria · `P1` important, should land in-phase but not a hard blocker · `P2` can slip to a later phase without breaking downstream work.
- **Estimate**: story points, Fibonacci (1, 2, 3, 5, 8, 13). Rough sizing for planning, not a commitment.
- **Depends on**: other ticket IDs that must be done first. Cross-epic dependencies are called out explicitly; within-epic ordering is mostly top-to-bottom.
- **Definition of Done** (applies to every story, per [../TESTING.md](../TESTING.md) §5): implementation + tests + integration verified + errors handled + security considered + logging where appropriate + docs updated + migrations included + frontend/backend both working + build passes + tests pass. A story is not "Done" on code existing alone.
- **Components/labels**: correspond to the module boundaries in [../ARCHITECTURE.md](../ARCHITECTURE.md) §4 (`auth`, `profile`, `goal`, `weight`, `progress`, `training`, `nutrition`, `checkin`, `coaching`, `conversation`, `memory`, `behavior`, `ai`, `whatsapp`, `notification`, `subscription`, `admin`, `safety`, `analytics`, `platform`, `frontend`, `infra`).

## Epics

| Epic | Title | File | Stories | Phase exit criteria (summary) |
|---|---|---|---|---|
| E01 | Foundation & Repository Setup | [EPIC-01-foundation.md](EPIC-01-foundation.md) | 10 | `docker compose up` gives a running empty-but-wired stack; CI green |
| E02 | Authentication & Identity | [EPIC-02-auth.md](EPIC-02-auth.md) | 11 | Full auth API tested incl. cross-user isolation |
| E03 | Profile, Goals, Weight & Onboarding | [EPIC-03-profile-goals-weight.md](EPIC-03-profile-goals-weight.md) | 12 | User can onboard, set a 10kg goal, log weight, see correct trend |
| E04 | AI Foundation | [EPIC-04-ai-foundation.md](EPIC-04-ai-foundation.md) | 11 | Text-in/validated-text-out flow works E2E in a test harness, fully cost-logged |
| E05 | Memory & Behavior Engine | [EPIC-05-memory-behavior.md](EPIC-05-memory-behavior.md) | 12 | The 3 product-spec memory scenarios pass as automated tests |
| E06 | Coaching Domains (Training/Nutrition/Check-ins) | [EPIC-06-coaching-domains.md](EPIC-06-coaching-domains.md) | 14 | AI can read/update plans, log workouts/meals, run check-ins via tools |
| E07 | WhatsApp Integration | [EPIC-07-whatsapp.md](EPIC-07-whatsapp.md) | 13 | Full critical E2E scenario passes against WhatsApp sandbox |
| E08 | Stripe & Billing | [EPIC-08-billing.md](EPIC-08-billing.md) | 10 | Trial→paid→cancel→resubscribe works via webhook-driven state |
| E09 | Dashboard, Admin & Analytics | [EPIC-09-dashboard-admin-analytics.md](EPIC-09-dashboard-admin-analytics.md) | 14 | Admin can view/pause/resolve; cost & business metrics accurate |
| E10 | Security & Privacy Hardening | [EPIC-10-hardening.md](EPIC-10-hardening.md) | 11 | SECURITY.md checklist verified in staging; load test documented |
| E11 | Production Launch (AWS) | [EPIC-11-production-launch.md](EPIC-11-production-launch.md) | 10 | Production smoke test of critical scenario passes on live creds, small cohort |

**Total: 128 stories.**

## Key Cross-Epic Dependencies
- E04 (AI Foundation) and E05 (Memory) both require E02 (auth/identity) for user context, and are prerequisites for E06's AI-driven tools and E07 (WhatsApp) end-to-end.
- E07 (WhatsApp) requires E04 + E05 + E06 complete (the orchestrator needs real domain tools and memory to be worth wiring to a live channel).
- E08 (Billing) is independent of E04-E07 and can be built in parallel by a second workstream once E02 (auth) exists — access gating (subscription required for WhatsApp coaching) is the only hard join point, wired at the end of E08.
- E09 (Admin/Analytics) depends on every domain it surfaces (E02-E08) having real data to show, but its own scaffolding (admin auth, base dashboard shell) can start earlier in parallel.
- E10 (Hardening) and E11 (Launch) are sequential finishers and assume E01-E09 are functionally complete.

## Suggested Parallelization (2-3 engineer team)
- **Stream A (backend core)**: E01 → E02 → E03 → E04 → E05 → E06
- **Stream B (frontend + billing)**: E01 (frontend half) → E03 (frontend half) → E08 → E09 (frontend half), joining Stream A once domain APIs exist
- **Stream C (integration)**: starts E07 once E04-E06 are stable, then joins for E10/E11

This is illustrative — actual sequencing depends on team size, which wasn't specified; the epic dependency graph above is what actually constrains ordering.
