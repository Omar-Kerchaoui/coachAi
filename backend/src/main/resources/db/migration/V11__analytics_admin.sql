-- Template vs. session distinction needed for the WhatsApp cost dashboard
-- (docs/backlog L10-912, Meta's differentiated per-conversation-type
-- pricing) — the opt-in template send was also never recorded as a
-- whatsapp_messages row at all until this epic, so template cost was
-- invisible even in principle.
ALTER TABLE whatsapp_messages ADD COLUMN message_type VARCHAR(10) NOT NULL DEFAULT 'SESSION';

-- ANALYTICS domain (docs/DATABASE.md, docs/BUSINESS_LOGIC.md §3)
-- created_at (not occurred_at, despite docs/DATABASE.md's naming) to match
-- BaseEntity's convention every other entity uses — the same mismatch bit
-- notification_events in Epic 7.
CREATE TABLE analytics_events (
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    metadata   JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_analytics_events_type_created ON analytics_events (event_type, created_at DESC);
CREATE INDEX idx_analytics_events_user_id ON analytics_events (user_id);

-- Nightly rollup — the business dashboard reads only this table, never
-- aggregates ai_requests/subscriptions/messages live on the request path
-- (docs/backlog L10-910).
CREATE TABLE business_metrics_daily (
    id                   UUID PRIMARY KEY,
    metric_date          DATE NOT NULL,
    mrr_cents            BIGINT NOT NULL DEFAULT 0,
    arr_cents            BIGINT NOT NULL DEFAULT 0,
    active_subscribers   INTEGER NOT NULL DEFAULT 0,
    trialing_subscribers INTEGER NOT NULL DEFAULT 0,
    new_subscribers      INTEGER NOT NULL DEFAULT 0,
    churned_subscribers  INTEGER NOT NULL DEFAULT 0,
    trial_conversions    INTEGER NOT NULL DEFAULT 0,
    dau                  INTEGER NOT NULL DEFAULT 0,
    wau                  INTEGER NOT NULL DEFAULT 0,
    stalled_users        INTEGER NOT NULL DEFAULT 0,
    at_risk_users        INTEGER NOT NULL DEFAULT 0,
    milestones_reached   INTEGER NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_business_metrics_daily_date ON business_metrics_daily (metric_date);

-- ADMIN domain
CREATE TABLE admin_notes (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    admin_id   UUID NOT NULL REFERENCES users (id),
    note       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_notes_user_id ON admin_notes (user_id, created_at DESC);

-- No separate admin_actions table: audit_logs (V1__baseline.sql) already
-- has actor_id/actor_type/action/target_type/target_id/metadata — the
-- exact same shape docs/DATABASE.md describes for admin_actions. Every
-- admin action in this epic is logged through the existing
-- AuditLogService instead of a parallel writer.
-- started_at isn't a separate column: BaseEntity's created_at already means
-- exactly that (the row is created the moment the intervention starts) —
-- a distinct started_at would just duplicate it. Caught via a live test:
-- BaseEntity unconditionally selects created_at, which this table didn't
-- have, throwing on the very first read.
CREATE TABLE human_interventions (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    admin_id   UUID NOT NULL REFERENCES users (id),
    reason     TEXT,
    ended_at   TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_human_interventions_user_id ON human_interventions (user_id, created_at DESC);
