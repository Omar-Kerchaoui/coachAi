# Epic L10-E11: Production Launch (AWS)

**Maps to:** ROADMAP.md Phase 11
**Goal:** Real production infrastructure on AWS, live monitoring, verified backups, legal review complete, and a staged rollout.
**Exit criteria:** Production smoke test of the full critical scenario with live (not sandbox) WhatsApp/Stripe on a small cohort before general availability.

---

### L10-1101 — AWS account/VPC/networking setup
`Priority: P0` · `Estimate: 5` · `Depends on: E10 complete` · `Component: infra`

VPC with public/private subnets, security groups per tier, NAT gateway, per DEPLOYMENT.md §2.

**Acceptance Criteria**
- No database or cache resource is internet-reachable (verified by security group review).

---

### L10-1102 — IaC for core infra (Terraform or CDK)
`Priority: P0` · `Estimate: 8` · `Depends on: L10-1101` · `Component: infra`

RDS Postgres (pgvector, Multi-AZ), ElastiCache Redis, ECS cluster + services (API + worker), ALB, ECR, S3, Secrets Manager, SES, Route 53, CloudFront, ACM certs.

**Acceptance Criteria**
- Entire staging environment can be created/destroyed from IaC alone (proves reproducibility).
- Production environment provisioned from the same IaC modules (environment-parameterized, not duplicated by hand).

---

### L10-1103 — CI/CD deploy pipeline to ECS (staging + production promotion gate)
`Priority: P0` · `Estimate: 5` · `Depends on: L10-1102, L10-109` · `Component: infra`

Merge→build→push ECR→deploy staging automatically→manual promotion to production.

**Acceptance Criteria**
- A staging deploy completes automatically on merge to main.
- Production deploy requires an explicit manual approval step (verified — cannot be bypassed by pipeline config alone).

---

### L10-1104 — SES production access + transactional email templates
`Priority: P0` · `Estimate: 3` · `Depends on: L10-1102` · `Component: platform`

Welcome, verification, password reset, subscription, payment failure, cancellation, deletion emails (product spec §92) — WhatsApp remains primary, email is not depended on for coaching.

**Acceptance Criteria**
- SES out of sandbox mode for production sending.
- Each listed email type fires at its correct trigger point (audit against E02/E08 code paths).

---

### L10-1105 — Production Stripe + WhatsApp live credentials cutover
`Priority: P0` · `Estimate: 3` · `Depends on: L10-1102` · `Component: subscription, whatsapp`

Live Stripe account verification, live WhatsApp Business phone number verification, production webhook endpoints registered, secrets loaded into Secrets Manager.

**Acceptance Criteria**
- Test-mode credentials fully absent from production configuration (verified).

---

### L10-1106 — Backup restore drill
`Priority: P0` · `Estimate: 3` · `Depends on: L10-1102` · `Component: infra`

Restore an RDS snapshot to a scratch instance and verify data integrity, per DEPLOYMENT.md §3.

**Acceptance Criteria**
- Documented, timed restore drill completed successfully at least once before launch; runbook written.

---

### L10-1107 — Legal page review (ToS, Privacy Policy, Cookie Policy, Health Disclaimer, AI Disclaimer)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-1006` · `Component: platform`

Human/legal counsel review per product spec §91 — explicitly not auto-approved.

**Acceptance Criteria**
- Sign-off obtained from legal counsel before production goes fully live; sign-off recorded.

---

### L10-1108 — DNS/TLS cutover + CDN configuration
`Priority: P0` · `Estimate: 2` · `Depends on: L10-1102` · `Component: infra`

Route 53 domain, ACM certs, CloudFront distribution for the frontend.

**Acceptance Criteria**
- HTTPS enforced end-to-end; HTTP requests redirect to HTTPS.

---

### L10-1109 — Production monitoring & alerting live
`Priority: P0` · `Estimate: 3` · `Depends on: L10-1008, L10-1102` · `Component: infra`

CloudWatch alarms routed to a real on-call notification channel in production (not just staging).

**Acceptance Criteria**
- A deliberately induced production-adjacent failure (in a controlled pre-launch test) triggers the correct alert to the correct channel.

---

### L10-1110 — Staged rollout & production smoke test
`Priority: P0` · `Estimate: 5` · `Depends on: all above` · `Component: platform`

Full critical scenario (TESTING.md §2) executed against live production infrastructure with real (small-cohort) users before general availability.

**Acceptance Criteria**
- End-to-end scenario passes on live Stripe + live WhatsApp with a small real cohort, no CRITICAL issues observed, before opening to general signups.
