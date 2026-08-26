# ECR repository — CI builds and pushes tagged backend images here
# (docs/backlog L10-1103); ECS pulls from here, never Docker Hub, in
# production. The frontend is a static Vite build shipped straight to S3 +
# CloudFront (docs/DEPLOYMENT.md §2), not a container — no frontend ECR
# repo needed.

resource "aws_ecr_repository" "backend" {
  name                 = "lose10-backend"
  image_tag_mutability = "IMMUTABLE" # a tag, once pushed, is never silently replaced — deploys are traceable to an exact image

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "backend" {
  repository = aws_ecr_repository.backend.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the last 20 images, expire the rest"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 20
      }
      action = { type = "expire" }
    }]
  })
}
