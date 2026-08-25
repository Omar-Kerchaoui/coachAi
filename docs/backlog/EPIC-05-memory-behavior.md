# Epic L10-E05: Memory & Behavior Engine

**Maps to:** ROADMAP.md Phase 5
**Goal:** Persistent, structured, semantically-retrievable customer memory — the product's core differentiator — plus behavioral pattern learning.
**Exit criteria:** The three product-spec memory scenarios (Friday overeating, running dislike, wedding date) pass as automated tests; retrieved memories visibly and naturally influence AI responses in the test harness.

---

### L10-501 — `memories`/`memory_events` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: memory`

Per DATABASE.md: category, key, content, importance, confidence, source_message_id, expires_at, last_used_at, metadata.

**Acceptance Criteria**
- Migration applies cleanly, indexed on `(user_id, category)`.
- Partial index for active (non-expired) memories per DATABASE.md §3.

---

### L10-502 — `memory_embeddings` schema + pgvector HNSW index
`Priority: P0` · `Estimate: 3` · `Depends on: L10-501` · `Component: memory`

`vector(N)` column, HNSW index with cosine ops, `model`/`dimension`/`embedding_version` columns.

**Acceptance Criteria**
- Migration applies cleanly against the pgvector-enabled Postgres.
- A seeded-data query proves cosine similarity search returns sane nearest neighbors.

---

### L10-503 — `conversation_threads`/`messages`/`conversation_summaries` schema
`Priority: P0` · `Estimate: 3` · `Depends on: L10-201` · `Component: conversation`

**Acceptance Criteria**
- Migration applies cleanly, indexed on `(thread_id, created_at DESC)`.

---

### L10-504 — Message persistence in the orchestrator pipeline
`Priority: P0` · `Estimate: 3` · `Depends on: L10-410, L10-503` · `Component: conversation`

Inbound/outbound messages persisted around the orchestrator call (see ARCHITECTURE.md §5).

**Acceptance Criteria**
- Every orchestrator invocation results in both inbound and outbound `messages` rows, correctly ordered.

---

### L10-505 — `MemoryExtractor`: candidate detection & generality heuristic
`Priority: P0` · `Estimate: 5` · `Depends on: L10-501, L10-410` · `Component: memory`

Distinguishes durable/recurring statements from one-off conversational state (MEMORY.md §3).

**Acceptance Criteria**
- "I'm tired today" produces no memory; "I always feel exhausted on Mondays because..." produces a candidate — both as regression tests.

---

### L10-506 — `MemoryExtractor`: dedup, update & contradiction handling
`Priority: P0` · `Estimate: 8` · `Depends on: L10-505` · `Component: memory`

Matches candidates against existing memories by (category, key) and semantic similarity; creates, updates, or discards per MEMORY.md §4.

**Acceptance Criteria**
- The `preferred_training_time` 19:00→06:00 update scenario (product spec) results in one updated row + a `memory_events(UPDATED)` entry, not a duplicate.
- Contradiction handling preserves prior value in `memory_events`.

---

### L10-507 — Async extraction job wiring (background, off request path)
`Priority: P0` · `Estimate: 3` · `Depends on: L10-506` · `Component: memory, platform`

Runs post-conversation, not inline in the request path (protects latency and AI cost-critical path per AI_ARCHITECTURE.md).

**Acceptance Criteria**
- Orchestrator response time unaffected by extraction (extraction runs after response is sent).
- Job retryable/idempotent (re-running on the same message set doesn't duplicate memories).

---

### L10-508 — Embedding generation on memory create/update
`Priority: P0` · `Estimate: 2` · `Depends on: L10-502, L10-506, L10-404` · `Component: memory`

**Acceptance Criteria**
- Every created/updated memory has exactly one corresponding `memory_embeddings` row after the async pipeline completes.

---

### L10-509 — `MemoryRetriever`: structured pre-filter + semantic search
`Priority: P0` · `Estimate: 5` · `Depends on: L10-508` · `Component: memory`

`user_id` + active (non-expired) filter, then pgvector cosine search within that set (MEMORY.md §6).

**Acceptance Criteria**
- Query never returns another user's memories (explicit isolation test).
- Semantic search demonstrably returns relevant memories for a seeded scenario (e.g. a food question surfaces the running-dislike-adjacent preference memories, not unrelated ones).

---

### L10-510 — `MemoryRetriever`: ranking & top-N cap
`Priority: P0` · `Estimate: 3` · `Depends on: L10-509` · `Component: memory`

Weighted score (similarity + importance + confidence + recency-of-use), capped top-N, updates `last_used_at`.

**Acceptance Criteria**
- A HIGH-importance, high-confidence memory outranks a fresher but LOW-importance one in a constructed test case.
- Result count never exceeds the configured cap regardless of raw match count.

---

### L10-511 — Wire `MemoryRetriever` into `ContextBuilder` + `saveMemory`/`updateMemory`/`searchMemories` tools
`Priority: P0` · `Estimate: 5` · `Depends on: L10-510, L10-408` · `Component: ai, memory`

Completes the context assembly (memory section no longer stubbed) and exposes memory tools to the model per AI_ARCHITECTURE.md §4.

**Acceptance Criteria**
- The 3 product-spec scenarios pass end-to-end: memory created earlier is retrieved and naturally referenced later without the coach saying "according to my memory."
- Personality rule spot-checked (MEMORY.md §11) in the eval-style test suite.

---

### L10-512 — `behavior_patterns`/`behavior_events` schema + Behavior Engine batch job
`Priority: P1` · `Estimate: 8` · `Depends on: L10-501` · `Component: behavior`

Periodic aggregation job detecting recurring patterns (missed workouts, overeating triggers, sleep correlation, weekend fluctuation) per MEMORY.md §10.

**Acceptance Criteria**
- Seeded data with 3+ Friday skips produces a `friday_training_adherence = low`-style pattern row.
- Patterns feed into `ContextBuilder` only when relevant to current context (e.g. surfaced on a Friday, not every day).
