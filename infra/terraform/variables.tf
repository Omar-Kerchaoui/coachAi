variable "environment" {
  description = "Deployment environment — staging or production. Every resource in this module is parameterized by this rather than duplicated per environment (docs/backlog L10-1102)."
  type        = string
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment must be \"staging\" or \"production\"."
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "availability_zone_count" {
  description = "Number of AZs to spread public/private subnets across."
  type        = number
  default     = 2
}

variable "domain_name" {
  description = "Root domain (e.g. lose10.app). Route 53/ACM/CloudFront resources derive from this."
  type        = string
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.medium"
}

variable "db_name" {
  type    = string
  default = "lose10"
}

variable "db_master_username" {
  type    = string
  default = "lose10_admin"
}

variable "db_multi_az" {
  description = "Multi-AZ RDS. Should be true for production (docs/DEPLOYMENT.md §3 — \"must never be single-copy\"); a single-AZ staging instance is a reasonable, explicit cost tradeoff."
  type        = bool
  default     = true
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "backend_image_tag" {
  description = "Tag of the backend image in ECR to deploy — set per-deploy by CI (docs/backlog L10-1103), not a fixed default."
  type        = string
  default     = "latest"
}

variable "backend_desired_count" {
  description = "ECS Fargate task count for the API service. docs/DEPLOYMENT.md §2 requires >=2 for availability."
  type        = number
  default     = 2
}

variable "worker_desired_count" {
  type    = number
  default = 1
}

variable "backend_cpu" {
  type    = number
  default = 512
}

variable "backend_memory" {
  type    = number
  default = 1024
}
