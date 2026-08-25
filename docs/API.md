# LOSE10 — API Specification (v1)

All endpoints under `/api/v1`. JSON in/out. Auth via `Authorization: Bearer <access_token>` (short-lived JWT) with rotating refresh tokens. All list endpoints paginated (`?page=&size=`, max size 100). All write endpoints validate DTOs server-side (Bean Validation) regardless of frontend validation.

## Error Format (uniform)
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Resource not found",
  "requestId": "a1b2c3d4",
  "details": []
}
```
No stack traces, no internal exception messages ever returned to the client.

## `/api/v1/auth`
| Method | Path | Purpose |
|---|---|---|
| POST | `/register` | Create account (email + password). Sends verification email. |
| POST | `/login` | Returns access + refresh token. |
| POST | `/logout` | Revokes current refresh token / session. |
| POST | `/refresh` | Rotates refresh token, issues new access token. Reuse of a revoked token revokes the whole token family. |
| POST | `/verify-email` | Consumes email verification token. |
| POST | `/password-reset/request` | Sends reset email (always 200, no user enumeration). |
| POST | `/password-reset/confirm` | Consumes reset token, sets new password. |

## `/api/v1/users`
| Method | Path | Purpose |
|---|---|---|
| GET | `/me` | Current user + profile + preferences. |
| PATCH | `/me` | Update profile fields. |
| PATCH | `/me/preferences` | Coach style, units, notification prefs. |
| POST | `/me/consents` | Record a consent grant. |
| DELETE | `/me/consents/{type}` | Revoke a consent. |
| GET | `/me/export` | Trigger/produce full data export (async job → downloadable link, see BUSINESS_LOGIC.md §Data Export). |
| DELETE | `/me` | Request account deletion (soft-delete + scheduled purge per retention policy). |

## `/api/v1/goals`
| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Current + historical goals. |
| POST | `/` | Create goal (onboarding). |
| PATCH | `/{goalId}` | Adjust target/date (creates a `goal_events` audit entry). |
| GET | `/{goalId}/milestones` | Milestone history. |

## `/api/v1/weight`
| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Weight history (paginated, date range filter). |
| POST | `/` | Log a weight entry. |
| GET | `/trend` | Rolling-average trend series for charting. |

## `/api/v1/progress`
| Method | Path | Purpose |
|---|---|---|
| GET | `/summary` | Dashboard summary: current/target/lost/remaining/% progress/phase. |
| GET | `/snapshots` | Historical progress snapshots. |

## `/api/v1/training`
| Method | Path | Purpose |
|---|---|---|
| GET | `/plan` | Current training plan + today's workout. |
| GET | `/plan/history` | Prior plan versions. |
| POST | `/sessions` | Log a completed workout session. |
| GET | `/sessions` | Workout session history. |

## `/api/v1/nutrition`
| Method | Path | Purpose |
|---|---|---|
| GET | `/profile` | Nutrition preferences/restrictions. |
| PATCH | `/profile` | Update. |
| GET | `/logs` | Nutrition logs (date range). |
| POST | `/logs/{date}/meals` | Add a meal (conversational text also supported via WhatsApp; this is the web equivalent). |

## `/api/v1/checkins`
| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Check-in history. |
| POST | `/{id}/complete` | Submit responses. |
| GET | `/weekly-reviews` | Weekly review history. |

## `/api/v1/conversations`
| Method | Path | Purpose |
|---|---|---|
| GET | `/thread` | Current thread + recent messages (web "peek at coach" view). |
| GET | `/thread/messages` | Paginated message history. |

## `/api/v1/memories`
| Method | Path | Purpose |
|---|---|---|
| GET | `/` | List own memories (category filter). Transparency feature. |
| DELETE | `/{id}` | User-initiated memory deletion (deletes structured row + embedding, audited). |

## `/api/v1/subscriptions`
| Method | Path | Purpose |
|---|---|---|
| GET | `/me` | Current subscription status. |
| POST | `/checkout-session` | Create Stripe Checkout session. |
| POST | `/portal-session` | Create Stripe customer portal session. |
| POST | `/cancel` | Cancel at period end. |

## `/api/v1/whatsapp`
| Method | Path | Purpose |
|---|---|---|
| GET | `/webhook` | Meta webhook verification handshake. |
| POST | `/webhook` | Inbound message/status webhook (signature-verified, idempotent). |
| POST | `/connect` | Initiate phone connection (sends opt-in template). |
| POST | `/disconnect` | Disconnect WhatsApp account. |

## `/api/v1/admin` (role: ADMIN or SUPPORT, scoped further per action)
| Method | Path | Purpose |
|---|---|---|
| GET | `/users` | Search/list users. |
| GET | `/users/{id}` | Full user detail view (profile, goal, memories, conversations, safety, subscription). |
| POST | `/users/{id}/coaching-state` | Set `AI_ACTIVE/HUMAN_ACTIVE/PAUSED`. |
| POST | `/users/{id}/notes` | Add admin note. |
| POST | `/conversations/{threadId}/messages` | Admin sends a manual message (requires `HUMAN_ACTIVE`). |
| GET | `/safety-events` | Safety review queue. |
| POST | `/safety-events/{id}/resolve` | Resolve with notes. |
| GET | `/metrics/business` | MRR/churn/retention/etc. (see BUSINESS_LOGIC.md). |
| GET | `/metrics/ai-cost` | AI cost dashboard data. |
| GET | `/metrics/whatsapp-cost` | WhatsApp cost dashboard data. |

## Webhooks Consumed (not part of the public API surface, but documented here)
- `POST /api/v1/whatsapp/webhook` — Meta Cloud API, HMAC-SHA256 signature verification against `X-Hub-Signature-256`.
- `POST /api/v1/stripe/webhook` — Stripe, signature verification via `Stripe-Signature` header and webhook signing secret. Raw body required (no JSON parsing before verification).

## Conventions
- All timestamps ISO-8601 UTC on the wire; converted to user timezone client-side.
- All monetary amounts in minor units (cents) + ISO currency code.
- Rate limits documented per-endpoint-class in [SECURITY.md](SECURITY.md) §Rate Limiting.
