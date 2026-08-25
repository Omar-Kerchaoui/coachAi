# LOSE10 — Memory & Vector Architecture

Memory is the product's core competitive advantage. This document covers what gets remembered, how, and how it's retrieved.

## 1. Memory Levels

| Level | Storage | Lifespan | Example |
|---|---|---|---|
| Short-term | Last N messages, loaded directly | Current turn | "I'm tired today" |
| Medium-term | `conversation_summaries` | Rolling window (e.g. per week) | Compressed summary of the week's conversation |
| Long-term (semantic) | `memories` + `memory_embeddings` (pgvector) | Until superseded/expired/deleted | "User dislikes running" |
| Structured state | Domain tables (goals, weight, plans, subscriptions, etc.) | Authoritative, always current | Current goal, current weight |

The AI context is built from all four layers (see AI_ARCHITECTURE.md §3) — semantic memory is not a substitute for structured state; it captures the qualitative, narrative facts structured tables can't hold.

## 2. Memory Categories
`PROFILE, GOAL, PREFERENCE, BEHAVIOR, LIFESTYLE, MOTIVATION, EVENT, COACHING_PREFERENCE, RELATIONSHIP_CONTEXT, OTHER`

## 3. The Core Principle: Don't Remember Everything

A message like "I'm tired today" is conversational state, not a memory — it's relevant for the next few turns and then irrelevant. A message like "I always feel exhausted on Mondays because I sleep late on Sundays" describes a recurring pattern and is memory-worthy.

The distinguishing signal `MemoryExtractor` looks for is **generality/recurrence** ("I always...", "usually...", explicit preferences, stated facts about life circumstances, named events) versus **one-off state** (today's mood, today's soreness, a single day's food).

## 4. Memory Extraction Pipeline (async, post-conversation)

Runs as a background job after a conversation turn (or batched at end of session/day for efficiency — implementation detail, but never inline in the request path, to keep response latency low and keep this off the AI cost-critical path).

```
MemoryExtractor.process(threadId, newMessages)
  1. Candidate detection: ask the model (cheap/fast call) — "does this message contain
     durable information worth remembering?" with the recurrence/generality heuristic above.
  2. If yes: extract structured candidate { category, key, content, importance, confidence }.
  3. Conflict check: query existing memories for this user by (category, key) and by semantic
     similarity to the candidate content.
  4. Decision:
       - No match found              → CREATE new memory
       - Match found, compatible     → UPDATE existing memory, append memory_events(UPDATED)
       - Match found, contradicts    → newest reliably-stated info supersedes; UPDATE + memory_events(CONTRADICTED)
       - Low confidence / ambiguous  → DISCARD (do not create low-value noise)
  5. Enqueue embedding generation for created/updated memory content.
  6. Record analytics event (MEMORY_CREATED / MEMORY_UPDATED).
```

### Example (dedup/update, per product spec)
Existing: `PREFERENCE / preferred_training_time = "19:00"`
New message: "I changed jobs and now I train at 6am."
Result: **UPDATE** the existing memory's `content` to `06:00`, not a new row; `memory_events` keeps the prior value for history.

## 5. Importance & Confidence

- **Importance** (`LOW/MEDIUM/HIGH/CRITICAL`) affects retrieval ranking and retention — CRITICAL (e.g. safety-relevant facts) is never silently pruned/expired.
- **Confidence** (0.0–1.0) distinguishes: explicitly stated by user (high, ~0.9+), AI-inferred from context (medium, ~0.6-0.8), repeatedly observed pattern (increases confidence over time — see Behavior Engine), single weak inference (low, ~0.3-0.5, often discarded rather than stored). Weak inferences are not presented to the coach as fact.

## 6. Retrieval Pipeline

```
MemoryRetriever.retrieve(userId, message, intent)
  1. Determine relevant categories from intent (e.g. a food question → PREFERENCE, LIFESTYLE)
  2. Structured pre-filter: WHERE user_id = ? AND (expires_at IS NULL OR expires_at > now())
                             AND category IN (relevant categories)
  3. Semantic search: embed the current message, pgvector cosine search within the
     pre-filtered set, top-K candidates (K ~20)
  4. Re-rank candidates by weighted score:
        score = w1*similarity + w2*importance + w3*confidence + w4*recency(last_used_at)
  5. Return top-N (typically 3-8) to stay within the context token budget
  6. Update last_used_at on returned memories (recency-of-use, not just recency-of-creation)
```

Retrieval never returns every semantic match above a raw threshold — it's ranked and capped, per product spec §18-19.

## 7. Vector Storage & Indexing

See DATABASE.md §4 for the full index rationale. Summary:
- `memory_embeddings.embedding vector(N)`, HNSW index, cosine distance.
- One embedding per memory row (1:1), regenerated whenever `memories.content` is updated.
- `model`, `dimension`, `embedding_version` stored per row — enables safe model migration (see §9).

## 8. Memory Deletion

- User-initiated (`DELETE /api/v1/memories/{id}`) or admin-initiated: deletes the `memories` row, its `memory_embeddings` row, and writes a `memory_events(DELETED)` + `audit_logs` entry. Hard delete, not soft — this is a privacy-sensitive user right.
- Account deletion cascades: all memories, embeddings, and events for the user are purged per the retention policy (see BUSINESS_LOGIC.md / SECURITY.md Privacy sections).
- Expiration (`expires_at`): set on time-bound memories (e.g. `EVENT` category like "wedding Dec 15" expires shortly after the date) — a nightly job marks expired memories inactive (excluded from retrieval) rather than deleting immediately, preserving history for a grace period before hard deletion.

## 9. Embedding Model Versioning & Migration

- Every embedding row records the model/version it was generated with.
- If the embedding model changes, a background re-embedding job walks existing `memories` and generates new vectors under the new model/version, writing new rows (or versioned columns) rather than mixing dimensions in one index — queries during migration filter by `embedding_version` until the migration completes, then the index is rebuilt against the new version only.

## 10. Behavioral Learning (Behavior Engine)

Distinct from single memories: the Behavior Engine aggregates `behavior_events` over time into `behavior_patterns` (e.g. repeated Friday skips → `friday_training_adherence = low`). It runs as a periodic batch job (not per-message), looking for:
- Recurring missed workouts / check-ins (day-of-week or trigger-based)
- Recurring overeating or specific food/time triggers
- Sleep-adherence correlations
- Weekend weight fluctuation patterns
- Successful habits worth reinforcing

Patterns feed into `ContextBuilder` (only when relevant to the current moment — e.g. surfacing the Friday pattern on a Friday) and can seed proactive coaching (see WHATSAPP.md §Proactive Scheduler). This is explicitly behavioral coaching pattern-matching, not clinical/psychological diagnosis (product spec §22).

## 11. Personality Rule (memory expression)

The coach never says "according to my memory/profile." Retrieved memories inform phrasing naturally (see AI_ARCHITECTURE.md §8). This is enforced in the system prompt and spot-checked via eval-style tests on canned conversation scenarios (see TESTING.md).
