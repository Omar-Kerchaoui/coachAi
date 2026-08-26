# Secrets Manager (docs/backlog L10-1009): every credential the app needs
# lives here, never in a task definition environment block in plaintext and
# never in source control. ECS task definitions reference these ARNs via
# the `secrets` block (see ecs.tf), which injects the resolved value as an
# env var at container start — the application code already reads every
# credential from an env var via Spring's @Value (verified by audit: no
# hardcoded secret exists anywhere in backend/src/main), so nothing in the
# app needs to change to consume these; this is purely how the env var gets
# populated in this environment vs. a local .env file.

resource "random_password" "db_master" {
  length  = 32
  special = false # RDS master password charset restrictions
}

resource "aws_secretsmanager_secret" "db_credentials" {
  name = "lose10/${var.environment}/db-credentials"
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = var.db_master_username
    password = random_password.db_master.result
  })
}

# Application secrets (Claude, WhatsApp, Stripe, JWT signing key) are
# created here as empty placeholders — Terraform must never be the place
# real API keys are typed in (they'd land in state and in this repo's
# plan output). Populate the actual values out-of-band, once, via:
#   aws secretsmanager put-secret-value --secret-id lose10/<env>/app-secrets --secret-string '...'
# then leave lifecycle.ignore_changes so a later `terraform apply` doesn't
# stomp the real value back to an empty placeholder.
resource "aws_secretsmanager_secret" "app_secrets" {
  name = "lose10/${var.environment}/app-secrets"
}

resource "aws_secretsmanager_secret_version" "app_secrets" {
  secret_id = aws_secretsmanager_secret.app_secrets.id
  secret_string = jsonencode({
    AUTH_JWT_SECRET               = ""
    CLAUDE_API_KEY                = ""
    EMBEDDING_API_KEY             = ""
    WHATSAPP_ACCESS_TOKEN         = ""
    WHATSAPP_APP_SECRET           = ""
    WHATSAPP_VERIFY_TOKEN         = ""
    STRIPE_SECRET_KEY             = ""
    STRIPE_WEBHOOK_SIGNING_SECRET = ""
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

# DB credential rotation (docs/backlog L10-1009 AC: "rotation tested at
# least once without downtime") — the rotation Lambda itself is AWS's
# standard managed one for RDS single-user rotation; wiring it needs the
# Secrets Manager rotation SAR app deployed once per account, which is an
# account-level bootstrap step, not something this module should assume.
# Left as a documented next step rather than a resource that would fail
# `terraform apply` against an account that hasn't done that bootstrap:
#
# resource "aws_secretsmanager_secret_rotation" "db_credentials" {
#   secret_id           = aws_secretsmanager_secret.db_credentials.id
#   rotation_lambda_arn = aws_lambda_function.rds_rotation.arn
#   rotation_rules { automatically_after_days = 30 }
# }
