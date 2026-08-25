-- Baseline: extensions required by later migrations.
-- pgvector powers semantic memory search (docs/MEMORY.md); citext gives us
-- case-insensitive unique email addresses without application-level lower()
-- gymnastics.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS citext;

-- AUDIT domain (docs/DATABASE.md) — created early since every other module
-- writes to it from the moment it has anything worth auditing.
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    actor_id    UUID,
    actor_type  VARCHAR(20) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   UUID,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs (actor_id);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
