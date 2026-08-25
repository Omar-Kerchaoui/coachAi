-- USER domain
CREATE TABLE user_profiles (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    display_name   VARCHAR(100),
    date_of_birth  DATE,
    sex            VARCHAR(20),
    height_cm      NUMERIC(5,1),
    country        VARCHAR(2),
    timezone       VARCHAR(64) NOT NULL DEFAULT 'UTC',
    locale         VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_user_profiles_user_id ON user_profiles (user_id);

CREATE TABLE user_preferences (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    units       VARCHAR(10) NOT NULL DEFAULT 'METRIC',
    coach_style VARCHAR(20) NOT NULL DEFAULT 'BALANCED',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_user_preferences_user_id ON user_preferences (user_id);

CREATE TABLE user_settings (
    id                       UUID PRIMARY KEY,
    user_id                  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    notification_frequency   VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    quiet_hours_start        TIME,
    quiet_hours_end          TIME,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_user_settings_user_id ON user_settings (user_id);

CREATE TABLE user_consents (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    consent_type VARCHAR(50) NOT NULL,
    version      VARCHAR(20) NOT NULL,
    source       VARCHAR(50),
    granted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_consents_user_id ON user_consents (user_id);
CREATE INDEX idx_user_consents_user_type ON user_consents (user_id, consent_type);

-- USER GOALS domain
CREATE TABLE goals (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    goal_type           VARCHAR(30) NOT NULL DEFAULT 'LOSE_WEIGHT',
    starting_weight_kg  NUMERIC(5,2) NOT NULL,
    target_weight_kg    NUMERIC(5,2) NOT NULL,
    target_date         DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
-- Enforces "exactly one ACTIVE goal per user" at the database level as a
-- backstop to the service-layer check (docs/backlog L10-304 AC).
CREATE UNIQUE INDEX uq_goals_one_active_per_user ON goals (user_id) WHERE status = 'ACTIVE';

CREATE TABLE goal_milestones (
    id           UUID PRIMARY KEY,
    goal_id      UUID NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    percentage   INTEGER NOT NULL,
    reached_at   TIMESTAMPTZ,
    notified_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_goal_milestones_goal_pct ON goal_milestones (goal_id, percentage);

CREATE TABLE goal_events (
    id         UUID PRIMARY KEY,
    goal_id    UUID NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    payload    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_events_goal_id ON goal_events (goal_id, created_at DESC);

-- WEIGHT domain
CREATE TABLE weight_entries (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight_kg   NUMERIC(5,2) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    source      VARCHAR(20) NOT NULL DEFAULT 'USER_REPORTED',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_weight_entries_user_recorded ON weight_entries (user_id, recorded_at DESC);

CREATE TABLE body_measurements (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    measurement_type VARCHAR(30) NOT NULL,
    value            NUMERIC(6,2) NOT NULL,
    unit             VARCHAR(10) NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_body_measurements_user_recorded ON body_measurements (user_id, recorded_at DESC);

-- PROGRESS domain
CREATE TABLE progress_snapshots (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    period_start           DATE NOT NULL,
    period_end             DATE NOT NULL,
    rolling_avg_weight_kg  NUMERIC(5,2),
    trend_kg_per_week      NUMERIC(5,3),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_progress_snapshots_user_period ON progress_snapshots (user_id, period_end DESC);
CREATE UNIQUE INDEX uq_progress_snapshots_user_period_end ON progress_snapshots (user_id, period_end);

CREATE TABLE progress_metrics (
    id            UUID PRIMARY KEY,
    snapshot_id   UUID NOT NULL REFERENCES progress_snapshots (id) ON DELETE CASCADE,
    metric_key    VARCHAR(50) NOT NULL,
    metric_value  NUMERIC(10,3) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_progress_metrics_snapshot_id ON progress_metrics (snapshot_id);
