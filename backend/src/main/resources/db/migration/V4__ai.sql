-- ANALYTICS domain (AI cost tracking slice — docs/DATABASE.md, docs/BUSINESS_LOGIC.md §5)
CREATE TABLE ai_requests (
    id                  UUID PRIMARY KEY,
    user_id             UUID REFERENCES users (id) ON DELETE SET NULL,
    provider            VARCHAR(30) NOT NULL,
    model               VARCHAR(50) NOT NULL,
    request_type        VARCHAR(30) NOT NULL,
    input_tokens        INTEGER NOT NULL DEFAULT 0,
    output_tokens       INTEGER NOT NULL DEFAULT 0,
    estimated_cost_usd  NUMERIC(10,6) NOT NULL DEFAULT 0,
    latency_ms          INTEGER,
    succeeded           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_requests_user_id ON ai_requests (user_id);
CREATE INDEX idx_ai_requests_created_at ON ai_requests (created_at DESC);
CREATE INDEX idx_ai_requests_model ON ai_requests (model);
