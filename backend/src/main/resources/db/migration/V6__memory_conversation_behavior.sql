-- CONVERSATIONS domain
CREATE TABLE conversation_threads (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel    VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversation_threads_user_id ON conversation_threads (user_id);

CREATE TABLE messages (
    id               UUID PRIMARY KEY,
    thread_id        UUID NOT NULL REFERENCES conversation_threads (id) ON DELETE CASCADE,
    direction        VARCHAR(10) NOT NULL,
    sender           VARCHAR(10) NOT NULL,
    content          TEXT NOT NULL,
    ai_metadata      JSONB,
    whatsapp_message_id VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_thread_created ON messages (thread_id, created_at DESC);

CREATE TABLE conversation_summaries (
    id           UUID PRIMARY KEY,
    thread_id    UUID NOT NULL REFERENCES conversation_threads (id) ON DELETE CASCADE,
    period_start TIMESTAMPTZ NOT NULL,
    period_end   TIMESTAMPTZ NOT NULL,
    summary_text TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversation_summaries_thread_id ON conversation_summaries (thread_id, period_end DESC);

-- MEMORY domain (docs/MEMORY.md)
CREATE TABLE memories (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category         VARCHAR(30) NOT NULL,
    key              VARCHAR(100) NOT NULL,
    content          TEXT NOT NULL,
    importance       VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    confidence       NUMERIC(3,2) NOT NULL DEFAULT 0.70,
    source_message_id UUID REFERENCES messages (id) ON DELETE SET NULL,
    expires_at       TIMESTAMPTZ,
    last_used_at     TIMESTAMPTZ,
    metadata         JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_memories_user_category ON memories (user_id, category);
-- Speeds up active-memory retrieval (docs/DATABASE.md §3). Not a partial
-- index on "expires_at IS NULL OR expires_at > now()" — Postgres requires
-- index predicates to be IMMUTABLE, and now() is only STABLE. A plain
-- composite index still lets the planner filter expiry efficiently.
CREATE INDEX idx_memories_user_expires ON memories (user_id, expires_at);
CREATE UNIQUE INDEX uq_memories_user_category_key ON memories (user_id, category, key);

CREATE TABLE memory_events (
    id                UUID PRIMARY KEY,
    memory_id         UUID NOT NULL REFERENCES memories (id) ON DELETE CASCADE,
    event_type        VARCHAR(20) NOT NULL,
    previous_content  TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_memory_events_memory_id ON memory_events (memory_id, created_at DESC);

CREATE TABLE memory_embeddings (
    id                UUID PRIMARY KEY,
    memory_id         UUID NOT NULL REFERENCES memories (id) ON DELETE CASCADE,
    embedding         vector(1024) NOT NULL,
    model             VARCHAR(50) NOT NULL,
    dimension         INTEGER NOT NULL,
    embedding_version INTEGER NOT NULL DEFAULT 1,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_memory_embeddings_memory_id ON memory_embeddings (memory_id);
-- HNSW chosen over IVFFlat: see docs/DATABASE.md §4 for the rationale
-- (better recall/latency at this scale, no periodic retrain step needed).
CREATE INDEX idx_memory_embeddings_hnsw ON memory_embeddings USING hnsw (embedding vector_cosine_ops);

-- BEHAVIOR domain
CREATE TABLE behavior_patterns (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pattern_key      VARCHAR(100) NOT NULL,
    description      TEXT NOT NULL,
    confidence       NUMERIC(3,2) NOT NULL DEFAULT 0.50,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    first_observed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_observed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_behavior_patterns_user_key ON behavior_patterns (user_id, pattern_key);

CREATE TABLE behavior_events (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pattern_id UUID REFERENCES behavior_patterns (id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    payload    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_behavior_events_user_id ON behavior_events (user_id, created_at DESC);
