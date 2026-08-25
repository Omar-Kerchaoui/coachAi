-- SAFETY domain (docs/DATABASE.md, docs/AI_ARCHITECTURE.md §7)
CREATE TABLE safety_events (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category      VARCHAR(50) NOT NULL,
    severity      VARCHAR(20) NOT NULL,
    detected_by   VARCHAR(20) NOT NULL DEFAULT 'RULE',
    source_text   TEXT,
    resolved      BOOLEAN NOT NULL DEFAULT false,
    resolved_at   TIMESTAMPTZ,
    resolved_by   UUID,
    resolution    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_safety_events_user_id ON safety_events (user_id);
CREATE INDEX idx_safety_events_unresolved ON safety_events (resolved, created_at DESC) WHERE resolved = false;
