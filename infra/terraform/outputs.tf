output "alb_dns_name" {
  value = aws_lb.backend.dns_name
}

output "api_url" {
  value = "https://${local.api_domain}"
}

output "site_url" {
  value = "https://${local.site_domain}"
}

output "ecr_backend_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "rds_endpoint" {
  value     = aws_db_instance.main.address
  sensitive = true
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.frontend.id
}
