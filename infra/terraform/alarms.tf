# CloudWatch alarms (docs/backlog L10-1008/L10-1109) routed to an SNS
# topic. What actually receives that topic (PagerDuty, Slack via a
# subscribed Lambda, email) is an operational decision for whoever runs
# on-call — an email subscription is wired here as the minimum viable
# "actually notifies a human" endpoint; swap/add subscriptions without
# touching the alarms themselves.
#
# The 5xx/RDS/ECS alarms below work purely from infrastructure-level
# metrics ALB/RDS/ECS emit automatically — no application code changes
# needed. AI-error-rate and WhatsApp-webhook-failure alarms (also called
# for in docs/DEPLOYMENT.md §5) would need the backend to publish custom
# metrics via Micrometer's CloudWatch registry; that Java-side wiring
# doesn't exist yet in this codebase, so those two alarms are left as a
# documented gap rather than referencing a metric namespace nothing
# publishes to (an alarm on a metric that never arrives just sits in
# INSUFFICIENT_DATA forever, which is worse than not having it — it looks
# covered without being covered).

variable "oncall_email" {
  description = "Email address CloudWatch alarms notify. Required before this module is applied for real — deliberately has no default."
  type        = string
}

resource "aws_sns_topic" "alarms" {
  name = "lose10-${var.environment}-alarms"
}

resource "aws_sns_topic_subscription" "oncall_email" {
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.oncall_email
}

resource "aws_cloudwatch_metric_alarm" "alb_5xx_rate" {
  alarm_name          = "lose10-${var.environment}-alb-5xx-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  period              = 60
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  threshold           = 10
  treat_missing_data  = "notBreaching"
  dimensions          = { LoadBalancer = aws_lb.backend.arn_suffix }
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_connections" {
  alarm_name          = "lose10-${var.environment}-rds-connection-saturation"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  period              = 60
  namespace           = "AWS/RDS"
  metric_name         = "DatabaseConnections"
  statistic           = "Average"
  # db.t4g.medium's default max_connections is in the low hundreds;
  # tune this once the real instance class for production is finalized.
  threshold          = 150
  treat_missing_data = "notBreaching"
  dimensions         = { DBInstanceIdentifier = aws_db_instance.main.identifier }
  alarm_actions      = [aws_sns_topic.alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "lose10-${var.environment}-rds-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  period              = 300
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  threshold           = 80
  treat_missing_data  = "notBreaching"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.main.identifier }
  alarm_actions       = [aws_sns_topic.alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "backend_running_task_deficit" {
  alarm_name          = "lose10-${var.environment}-backend-task-deficit"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 3
  period              = 60
  namespace           = "ECS/ContainerInsights"
  metric_name         = "RunningTaskCount"
  statistic           = "Average"
  threshold           = var.backend_desired_count
  treat_missing_data  = "breaching" # no data here means the service isn't reporting at all — treat as a failure, not "fine"
  dimensions          = { ClusterName = aws_ecs_cluster.main.name, ServiceName = aws_ecs_service.backend.name }
  alarm_actions       = [aws_sns_topic.alarms.arn]
}
