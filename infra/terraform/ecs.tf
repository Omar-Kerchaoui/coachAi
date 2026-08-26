# ECS Fargate: API service + worker service on the same image, different
# entrypoints (docs/DEPLOYMENT.md §2) — a slow background job (scheduler,
# memory extraction, embeddings, rollups) never starves API-serving
# capacity, and each scales independently.

resource "aws_ecs_cluster" "main" {
  name = "lose10-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/lose10-${var.environment}-backend"
  retention_in_days = 30
}

resource "aws_cloudwatch_log_group" "worker" {
  name              = "/ecs/lose10-${var.environment}-worker"
  retention_in_days = 30
}

# --- IAM: execution role (pulls image, writes logs, reads secrets) vs.
# task role (what the running application itself is allowed to do — kept
# separate and minimal, per least-privilege) ---

data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "lose10-${var.environment}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "execution_secrets" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.db_credentials.arn, aws_secretsmanager_secret.app_secrets.arn]
  }
}

resource "aws_iam_role_policy" "execution_secrets" {
  name   = "read-app-secrets"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_secrets.json
}

resource "aws_iam_role" "task" {
  name               = "lose10-${var.environment}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

data "aws_iam_policy_document" "task_s3" {
  statement {
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.media.arn}/*"]
  }
  statement {
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.media.arn]
  }
}

resource "aws_iam_role_policy" "task_s3" {
  name   = "media-bucket-access"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task_s3.json
}

locals {
  db_url = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.db_name}"

  # Every value here is either a plain non-secret config value or a
  # Secrets Manager reference resolved by the `secrets` block below — none
  # of these are real credentials at rest in this file or in state.
  common_environment = [
    { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
    { name = "DATABASE_URL", value = local.db_url },
    { name = "REDIS_HOST", value = aws_elasticache_cluster.main.cache_nodes[0].address },
    { name = "REDIS_PORT", value = tostring(aws_elasticache_cluster.main.cache_nodes[0].port) },
    { name = "CORS_ALLOWED_ORIGINS", value = "https://${local.site_domain}" },
  ]

  common_secrets = [
    { name = "DATABASE_PASSWORD", valueFrom = "${aws_secretsmanager_secret.db_credentials.arn}:password::" },
    { name = "AUTH_JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:AUTH_JWT_SECRET::" },
    { name = "CLAUDE_API_KEY", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:CLAUDE_API_KEY::" },
    { name = "EMBEDDING_API_KEY", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:EMBEDDING_API_KEY::" },
    { name = "WHATSAPP_ACCESS_TOKEN", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:WHATSAPP_ACCESS_TOKEN::" },
    { name = "WHATSAPP_APP_SECRET", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:WHATSAPP_APP_SECRET::" },
    { name = "WHATSAPP_VERIFY_TOKEN", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:WHATSAPP_VERIFY_TOKEN::" },
    { name = "STRIPE_SECRET_KEY", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:STRIPE_SECRET_KEY::" },
    { name = "STRIPE_WEBHOOK_SIGNING_SECRET", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:STRIPE_WEBHOOK_SIGNING_SECRET::" },
  ]
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "lose10-${var.environment}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name         = "backend"
    image        = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
    essential    = true
    portMappings = [{ containerPort = 8080, protocol = "tcp" }]
    environment  = local.common_environment
    secrets      = local.common_secrets
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health/liveness || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.backend.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "backend"
      }
    }
  }])
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "lose10-${var.environment}-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name        = "worker"
    image       = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
    essential   = true
    environment = concat(local.common_environment, [{ name = "APP_PROFILE_WORKER", value = "true" }])
    secrets     = local.common_secrets
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.worker.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "worker"
      }
    }
  }])
}

# --- ALB ---

resource "aws_lb" "backend" {
  name               = "lose10-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_lb_target_group" "backend" {
  name        = "lose10-${var.environment}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health/liveness"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.api.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# --- Services ---

resource "aws_ecs_service" "backend" {
  name            = "lose10-${var.environment}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  depends_on = [aws_lb_listener.https]
}

resource "aws_ecs_service" "worker" {
  name            = "lose10-${var.environment}-worker"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.worker.arn
  desired_count   = var.worker_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

# --- Autoscaling: API service scales on request count per target
# (docs/DEPLOYMENT.md §2 — "autoscale on CPU/request count") ---

resource "aws_appautoscaling_target" "backend" {
  max_capacity       = var.backend_desired_count * 3
  min_capacity       = var.backend_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.backend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "backend_cpu" {
  name               = "lose10-${var.environment}-backend-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  service_namespace  = aws_appautoscaling_target.backend.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 60
  }
}
