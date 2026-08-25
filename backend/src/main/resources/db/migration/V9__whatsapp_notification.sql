-- WHATSAPP domain
CREATE TABLE whatsapp_accounts (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    phone_e164     VARCHAR(20) NOT NULL,
    wa_id          VARCHAR(50),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    opted_in_at    TIMESTAMPTZ,
    opted_out_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_whatsapp_accounts_user_id ON whatsapp_accounts (user_id);
CREATE UNIQUE INDEX uq_whatsapp_accounts_phone ON whatsapp_accounts (phone_e164);

CREATE TABLE whatsapp_messages (
    id             UUID PRIMARY KEY,
    message_id     UUID REFERENCES messages (id) ON DELETE SET NULL,
    direction      VARCHAR(10) NOT NULL,
    wa_message_id  VARCHAR(100),
    status         VARCHAR(20) NOT NULL DEFAULT 'SENT',
    raw_payload    JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_whatsapp_messages_wa_message_id ON whatsapp_messages (wa_message_id) WHERE wa_message_id IS NOT NULL;

CREATE TABLE whatsapp_webhook_events (
    id                 UUID PRIMARY KEY,
    event_id           VARCHAR(200) NOT NULL,
    payload            JSONB NOT NULL,
    processed_at       TIMESTAMPTZ,
    processing_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_whatsapp_webhook_events_event_id ON whatsapp_webhook_events (event_id);

-- NOTIFICATIONS domain
CREATE TABLE notification_preferences (
    id                        UUID PRIMARY KEY,
    user_id                   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    morning_checkin_time      TIME,
    evening_checkin_time      TIME,
    workout_reminder_enabled  BOOLEAN NOT NULL DEFAULT true,
    quiet_hours_start         TIME,
    quiet_hours_end           TIME,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_notification_preferences_user_id ON notification_preferences (user_id);

CREATE TABLE scheduled_messages (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message_type    VARCHAR(30) NOT NULL,
    scheduled_for   TIMESTAMPTZ NOT NULL,
    -- Stored explicitly rather than indexed as scheduled_for::date: Postgres
    -- requires index expressions to be IMMUTABLE, and casting timestamptz to
    -- date depends on the session timezone (STABLE, not IMMUTABLE) — the
    -- same root cause as the V6 memories index fix. The application sets
    -- this to the user's local calendar date at creation time.
    scheduled_date  DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scheduled_messages_due ON scheduled_messages (status, scheduled_for);
-- Idempotency key: at most one of a given type per user per calendar day.
CREATE UNIQUE INDEX uq_scheduled_messages_idempotency ON scheduled_messages (user_id, message_type, scheduled_date);

CREATE TABLE notification_events (
    id                    UUID PRIMARY KEY,
    scheduled_message_id  UUID NOT NULL REFERENCES scheduled_messages (id) ON DELETE CASCADE,
    event_type            VARCHAR(30) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_events_scheduled_message_id ON notification_events (scheduled_message_id);
