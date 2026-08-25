# Epic L10-E02: Authentication & Identity

**Maps to:** ROADMAP.md Phase 2
**Goal:** Secure account creation, login, session/token lifecycle, and role-based authorization, with cross-user data isolation proven from the start.
**Exit criteria:** Full auth API tested, including cross-user isolation tests for authenticated resources.

---

### L10-201 — `users`/`user_roles` schema + Flyway migration
`Priority: P0` · `Estimate: 2` · `Depends on: L10-105` · `Component: auth`

Create `users`, `user_roles` tables per DATABASE.md. Password hash column, status enum, unique email/phone constraints.

**Acceptance Criteria**
- Migration applies cleanly; unique constraints on email and phone_e164 enforced at DB level.
- Repository layer uses `citext` (or equivalent) for case-insensitive email lookup.

---

### L10-202 — `sessions`/`refresh_tokens` schema + Flyway migration
`Priority: P0` · `Estimate: 2` · `Depends on: L10-201` · `Component: auth`

Create `sessions`, `refresh_tokens` (with `family_id`, `revoked_at`, `replaced_by`) per DATABASE.md.

**Acceptance Criteria**
- Migration applies cleanly with correct FKs/indexes to `users`.

---

### L10-203 — User registration (`POST /auth/register`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: auth`

Email + password registration, Argon2id/bcrypt hashing, input validation, sends verification email (stub/log in this epic if email infra isn't ready — real SES wiring can land with L10-1104 or earlier if convenient).

**Acceptance Criteria**
- Duplicate email registration rejected with a clear error code, no user enumeration beyond that (password reset flow specifically avoids enumeration per §205).
- Password never stored or logged in plaintext.
- Weak passwords rejected per a documented policy.

---

### L10-204 — Email verification flow
`Priority: P0` · `Estimate: 3` · `Depends on: L10-203` · `Component: auth`

Single-use, time-limited, hashed verification token; `POST /auth/verify-email`.

**Acceptance Criteria**
- Expired/used tokens rejected.
- Account marked verified only via this flow.

---

### L10-205 — Login (`POST /auth/login`) + access/refresh token issuance
`Priority: P0` · `Estimate: 3` · `Depends on: L10-202, L10-203` · `Component: auth`

Issues short-lived JWT access token + opaque refresh token (hashed at rest).

**Acceptance Criteria**
- Wrong password rejected with generic error (no "email exists but wrong password" leak).
- Rate limited per SECURITY.md §6 (public class).
- Successful login creates a `sessions` row.

---

### L10-206 — Refresh token rotation (`POST /auth/refresh`)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-205` · `Component: auth`

Rotating refresh tokens with family-based reuse detection — reuse of a revoked token revokes the whole family.

**Acceptance Criteria**
- Normal rotation issues a new refresh token and invalidates the old one.
- Replaying an already-rotated (revoked) token revokes the entire family and forces re-login (test explicitly).

---

### L10-207 — Logout (`POST /auth/logout`)
`Priority: P0` · `Estimate: 1` · `Depends on: L10-205` · `Component: auth`

Revokes current session/refresh token.

**Acceptance Criteria**
- Revoked refresh token can no longer be used to obtain a new access token.

---

### L10-208 — Password reset flow
`Priority: P0` · `Estimate: 3` · `Depends on: L10-203` · `Component: auth`

`POST /auth/password-reset/request` (always 200, no enumeration) + `/confirm` (single-use hashed token).

**Acceptance Criteria**
- Requesting reset for a non-existent email returns 200 with no observable timing/response difference.
- Reset token single-use, expires, and invalidates existing sessions/refresh tokens on successful reset.

---

### L10-209 — Role-based authorization (`USER/ADMIN/SUPPORT/COACH`)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: auth`

Spring Security method/URL security enforcing roles; roles never trusted from client input.

**Acceptance Criteria**
- Non-admin calling an admin-only endpoint gets 403.
- Role checks covered by tests for every role combination relevant at this stage.

---

### L10-210 — Account deletion request (soft-delete)
`Priority: P1` · `Estimate: 2` · `Depends on: L10-205` · `Component: auth`

`DELETE /users/me` marks account for scheduled purge per retention policy (full cascade deletion lands in E10 once all domains exist).

**Acceptance Criteria**
- Soft-deleted account can no longer log in.
- Deletion request is audit-logged (`USER_DELETED`).

---

### L10-211 — Cross-user data isolation test suite (auth-scoped resources)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-203..L10-209` · `Component: platform`

Automated tests proving User A cannot read/act on User B's session, profile stub, or auth-scoped resources — the first installment of the isolation suite that grows with every later epic (see SECURITY.md §3, TESTING.md §3).

**Acceptance Criteria**
- Suite runs in CI.
- Explicit test: token from User A rejected/ownership-checked against User B's resource ID.
