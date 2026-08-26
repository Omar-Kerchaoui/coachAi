# ElastiCache Redis — cache/locks/rate-limit/idempotency. Treated as
# disposable (docs/DEPLOYMENT.md §3: "no backup requirement, since nothing
# is solely stored there") — single node, no cluster mode, no backups.

resource "aws_elasticache_subnet_group" "main" {
  name       = "lose10-${var.environment}-redis-subnets"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_elasticache_cluster" "main" {
  cluster_id         = "lose10-${var.environment}"
  engine             = "redis"
  engine_version     = "7.1"
  node_type          = var.redis_node_type
  num_cache_nodes    = 1
  port               = 6379
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  apply_immediately  = var.environment != "production"
}
