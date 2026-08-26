# RDS Postgres 16 with pgvector — Multi-AZ, automated backups + PITR
# (docs/DEPLOYMENT.md §3: "the system of record and must never be
# single-copy"). pgvector ships as an extension available on RDS Postgres
# out of the box (no custom parameter group needed to enable the binary —
# `CREATE EXTENSION vector` runs from Flyway's V1 migration, already in
# this repo).

resource "aws_db_subnet_group" "main" {
  name       = "lose10-${var.environment}-db-subnets"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_db_instance" "main" {
  identifier     = "lose10-${var.environment}"
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  db_name  = var.db_name
  username = var.db_master_username
  password = random_password.db_master.result

  allocated_storage     = 50
  max_allocated_storage = 200 # storage autoscaling — avoids a manual resize under growth
  storage_type          = "gp3"
  storage_encrypted     = true

  multi_az               = var.db_multi_az
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:30-mon:05:30"

  # Production must never be destroyed by a stray `terraform destroy`;
  # staging is allowed to be, deliberately, since it exists to be torn
  # down/recreated for reproducibility testing (docs/backlog L10-1102 AC).
  deletion_protection = var.environment == "production"
  skip_final_snapshot = var.environment != "production"
  # Fixed name, not a timestamp() interpolation — timestamp() changes on
  # every plan and would make this attribute perpetually "changed" in
  # `terraform plan` even when nothing actually changed. A fixed name is
  # fine here: this only takes effect on the (rare, deliberate) destroy of
  # a production DB, not on every apply.
  final_snapshot_identifier = var.environment == "production" ? "lose10-production-final-snapshot" : null

  performance_insights_enabled = true
}
