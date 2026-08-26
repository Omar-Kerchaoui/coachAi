# VPC with public (ALB, NAT) and private (ECS, RDS, ElastiCache) subnets —
# the database and cache tiers are never internet-reachable
# (docs/DEPLOYMENT.md §2, docs/backlog L10-1101 AC).

data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "lose10-${var.environment}-vpc" }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "lose10-${var.environment}-igw" }
}

resource "aws_subnet" "public" {
  count                   = var.availability_zone_count
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 4, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = { Name = "lose10-${var.environment}-public-${count.index}" }
}

resource "aws_subnet" "private" {
  count             = var.availability_zone_count
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 4, count.index + var.availability_zone_count)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "lose10-${var.environment}-private-${count.index}" }
}

# One NAT gateway (not one per AZ) — private-subnet ECS tasks need outbound
# internet for Claude/WhatsApp/Stripe API calls; a single NAT is a
# deliberate cost/availability tradeoff acceptable at this scale, not an
# oversight. Revisit to one-per-AZ if NAT becomes a single point of failure
# in practice.
resource "aws_eip" "nat" {
  domain = "vpc"
  tags   = { Name = "lose10-${var.environment}-nat-eip" }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id
  tags          = { Name = "lose10-${var.environment}-nat" }
  depends_on    = [aws_internet_gateway.main]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
  tags = { Name = "lose10-${var.environment}-public-rt" }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }
  tags = { Name = "lose10-${var.environment}-private-rt" }
}

resource "aws_route_table_association" "public" {
  count          = var.availability_zone_count
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = var.availability_zone_count
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# --- Security groups: least-privilege per tier ---

resource "aws_security_group" "alb" {
  name_prefix = "lose10-${var.environment}-alb-"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "HTTP from internet (redirected to HTTPS at the listener, docs/backlog L10-1108)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "lose10-${var.environment}-alb-sg" }
}

resource "aws_security_group" "ecs_tasks" {
  name_prefix = "lose10-${var.environment}-ecs-"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "From ALB only"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "lose10-${var.environment}-ecs-sg" }
}

resource "aws_security_group" "rds" {
  name_prefix = "lose10-${var.environment}-rds-"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Postgres from ECS tasks only — never internet-reachable"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  tags = { Name = "lose10-${var.environment}-rds-sg" }
}

resource "aws_security_group" "redis" {
  name_prefix = "lose10-${var.environment}-redis-"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Redis from ECS tasks only — never internet-reachable"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  tags = { Name = "lose10-${var.environment}-redis-sg" }
}
