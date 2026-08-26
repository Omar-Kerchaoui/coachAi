terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state deliberately not configured here — bootstrapping it (an S3
  # bucket + DynamoDB lock table) is itself infrastructure that has to exist
  # before this backend block can point at it. Uncomment and fill in once
  # that bootstrap has been run once, by hand, for real
  # (docs/backlog L10-1101):
  #
  # backend "s3" {
  #   bucket         = "lose10-terraform-state"
  #   key            = "lose10/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "lose10-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "lose10"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
