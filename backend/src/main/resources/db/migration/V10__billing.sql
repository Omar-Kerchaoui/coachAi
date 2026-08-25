CREATE TABLE plans (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    stripe_price_id VARCHAR(100) NOT NULL,
    amount_cents    INTEGER NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'usd',
    billing_interval VARCHAR(10) NOT NULL,
    trial_days      INTEGER NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_plans_stripe_price_id ON plans (stripe_price_id);

-- stripe_subscription_id is populated only once Stripe's webhook confirms
-- the subscription exists (customer.subscription.created) — a row is
-- created at checkout-session time with it NULL and status INCOMPLETE so a
-- customer id (needed for the portal/checkout API calls) has somewhere to
-- live before that webhook arrives. Postgres unique indexes permit multiple
-- NULLs, so several pending checkouts don't collide.
CREATE TABLE subscriptions (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_id                UUID NOT NULL REFERENCES plans (id),
    stripe_customer_id     VARCHAR(100) NOT NULL,
    stripe_subscription_id VARCHAR(100),
    status                 VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETE',
    trial_end              TIMESTAMPTZ,
    current_period_end     TIMESTAMPTZ,
    cancel_at_period_end   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_subscriptions_stripe_subscription_id ON subscriptions (stripe_subscription_id);
CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions (stripe_customer_id);

CREATE TABLE subscription_events (
    id                UUID PRIMARY KEY,
    subscription_id   UUID REFERENCES subscriptions (id) ON DELETE SET NULL,
    stripe_event_id   VARCHAR(255) NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    payload           JSONB NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_subscription_events_stripe_event_id ON subscription_events (stripe_event_id);

CREATE TABLE coupons (
    id               UUID PRIMARY KEY,
    code             VARCHAR(50) NOT NULL,
    stripe_coupon_id VARCHAR(100) NOT NULL,
    discount_type    VARCHAR(10) NOT NULL,
    discount_value   NUMERIC(10,2) NOT NULL,
    valid_from       TIMESTAMPTZ,
    valid_until      TIMESTAMPTZ,
    max_redemptions  INTEGER,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_coupons_code ON coupons (code);

CREATE TABLE payments (
    id                       UUID PRIMARY KEY,
    subscription_id          UUID NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    stripe_payment_intent_id VARCHAR(100) NOT NULL,
    amount_cents             INTEGER NOT NULL,
    currency                 VARCHAR(3) NOT NULL,
    status                   VARCHAR(20) NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_payments_stripe_payment_intent_id ON payments (stripe_payment_intent_id);
CREATE INDEX idx_payments_subscription_id ON payments (subscription_id);

CREATE TABLE refunds (
    id               UUID PRIMARY KEY,
    payment_id       UUID NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    stripe_refund_id VARCHAR(100) NOT NULL,
    amount_cents     INTEGER NOT NULL,
    reason           VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_refunds_stripe_refund_id ON refunds (stripe_refund_id);
CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);

-- Seed the two plans from docs/backlog L10-802. Stripe price ids are
-- placeholders until real Stripe test-mode products are configured — the
-- application treats a blank STRIPE_SECRET_KEY the same way it already
-- does for Claude/WhatsApp (graceful "not configured" failure, not a crash).
INSERT INTO plans (id, name, stripe_price_id, amount_cents, currency, billing_interval, trial_days, active)
VALUES
    (gen_random_uuid(), 'Monthly', 'price_lose10_monthly_placeholder', 1999, 'usd', 'MONTH', 7, TRUE),
    (gen_random_uuid(), 'Annual', 'price_lose10_annual_placeholder', 19999, 'usd', 'YEAR', 7, TRUE);
