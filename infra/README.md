# LOSE10 Infrastructure (docs/backlog Epic 11)

## Status

**Written, `terraform validate`-clean, never applied.** No AWS resources
exist from this code. Provisioning real infrastructure means creating an
AWS account, agreeing to real spend, and making decisions (domain
registrar, budget, region, who holds the account) that are the project
owner's to make — not something to do autonomously from an agent session.

What's here is real, reviewable Terraform matching the architecture in
`docs/DEPLOYMENT.md` §2 — VPC, RDS (Postgres 16 + pgvector, Multi-AZ),
ElastiCache Redis, ECS Fargate (API + worker services) behind an ALB,
ECR, S3 (frontend static site + private media/export bucket), Secrets
Manager, SES, Route 53 + ACM + CloudFront, and CloudWatch alarms wired to
an SNS topic. `docs/DEPLOYMENT.md` §2 left the IaC tool choice open
("Terraform or CDK, decided at Phase 1 kickoff") — Terraform was used here
because it's what's actually installed and verifiable in this environment,
not because CDK was ruled out.

## Before this can actually be applied

1. **An AWS account** with billing set up, and credentials
   (`aws configure` / SSO) available to Terraform.
2. **A registered domain** (or an existing one) and its Route 53 hosted
   zone already created — `dns_cdn.tf` reads an *existing* zone via
   `data "aws_route53_zone"`, it doesn't create one.
3. **Bootstrap the remote state backend once, by hand** (an S3 bucket +
   DynamoDB lock table) — see the commented-out `backend "s3"` block in
   `versions.tf`. Chicken-and-egg: that bootstrap can't itself be managed
   by the Terraform it configures.
4. Fill in `staging.tfvars` / `production.tfvars` from the `.example`
   files (never commit the real ones — `.gitignore` already excludes
   them).
5. After the first `terraform apply`, populate real values into the
   `lose10/<env>/app-secrets` Secrets Manager secret out-of-band (`aws
   secretsmanager put-secret-value`) — Terraform deliberately seeds it
   with empty strings and never overwrites it again
   (`lifecycle.ignore_changes`), so a real API key never has to pass
   through a `.tf` file or plan output.

## What this does *not* cover (real-world actions, not code)

- **Legal review** of the pages under `frontend/src/pages/{Privacy,Terms}Page.tsx`
  (docs/backlog L10-1107) — needs actual legal counsel sign-off.
- **Live Stripe/WhatsApp credential cutover** (L10-1105) — needs a real
  business Stripe account and a verified WhatsApp Business phone number.
- **SES production access** (L10-1104) — domain verification DNS records
  are created here, but moving out of the SES sandbox is an AWS support
  ticket, not a Terraform resource.
- **Backup restore drill** (L10-1106) — needs a real RDS snapshot to
  restore, which only exists once this is actually applied and running.
- **Staged rollout / production smoke test with a real cohort** (L10-1110)
  — needs real users.

## Usage once the prerequisites above are met

```bash
cd infra/terraform
terraform init                              # first time only, after the S3 backend exists
terraform plan  -var-file=staging.tfvars
terraform apply -var-file=staging.tfvars
```

Never run `terraform apply` against `production.tfvars` without a second
person reviewing the plan output first — this provisions a real database
with real user data in it.
