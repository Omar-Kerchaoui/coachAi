# LOSE10 — AI Architecture

## 1. Provider Abstraction

```java
interface AIProvider {
    AIResponse generate(AIRequest request);           // supports tool calling, structured output
    AIResponse continueWithToolResults(...);           // tool-calling loop continuation
}

interface EmbeddingProvider {
    Embedding generateEmbedding(String text);
    List<Embedding> batchGenerateEmbeddings(List<String> texts);
}
```

- `ClaudeAIProvider implements AIProvider` — the only class in the codebase allowed to reference the Claude SDK.
- `ClaudeEmbeddingProvider implements EmbeddingProvider` (or a dedicated embedding model/provider — decided at implementation time based on quality/cost; abstraction means this is swappable without touching orchestration code).
- Every call wrapped with: timeout, retry with backoff (idempotent calls only), circuit breaker (fail closed to a safe fallback message, never fail open to an unvalidated response), and mandatory usage/cost recording to `ai_requests` (see DATABASE.md).
- No other module imports the Claude SDK or constructs prompts directly — all prompt construction lives in `ai.orchestrator`.

## 2. AIOrchestrator Pipeline

```
AIOrchestrator.handle(userId, inboundMessage)
  1. IntentDetector.detect(message, recentContext)
       → classifies: LOG_DATA | QUESTION | VENTING | SMALL_TALK | SAFETY_CONCERN | COMMAND (pause, change style, etc.)
  2. SafetyAnalyzer.screen(message, intent)
       → if HIGH/CRITICAL risk: short-circuit to a safe scripted response + create safety_event, skip generation
  3. ContextBuilder.build(userId, intent)
       → assembles the compact context block (see §3)
  4. MemoryRetriever.retrieve(userId, message, intent)
       → structured + semantic memory (see MEMORY.md)
  5. ToolRegistry.availableTools(userId, coachingState)
       → user-scoped, permission-checked tool set
  6. ClaudeAIProvider.generate(systemPrompt, context, tools, message)
       → tool-calling loop: execute requested tools server-side, feed results back, repeat until final text response
  7. ResponseValidator.validate(response, context)
       → see §5; on failure, fallback response, log for review, do NOT send unvalidated text
  8. return validated response
```

Each numbered stage is a distinct Spring bean with a single responsibility — no "one giant orchestrator method with everything inlined." `AIOrchestrator` composes them; it does not implement their logic.

## 3. Context Builder — what gets sent to Claude

A compact, structured block assembled per-request (never the full DB, never full conversation history):

```
USER PROFILE           (name, timezone, coach style)
CURRENT GOAL            (starting/current/target weight, days remaining)
CURRENT WEIGHT + TREND  (rolling average, direction)
COACHING STATE          (style: SUPPORTIVE/BALANCED/TOUGH_LOVE)
CURRENT TRAINING PLAN   (today's workout only, not the whole plan)
RELEVANT MEMORIES       (top-K from MemoryRetriever, see MEMORY.md — typically 3-8)
RELEVANT BEHAVIOR PATTERNS (only patterns relevant to detected intent, e.g. day-of-week patterns on that day)
RECENT CONVERSATION     (last ~10 messages, or a summary + last few if thread is long)
RECENT CHECK-INS        (last check-in only, not full history)
SAFETY CONTEXT          (any open, unresolved safety flags)
AVAILABLE TOOLS         (tool schemas, scoped to this user)
```

Token budget is enforced: each section has a soft cap; `ContextBuilder` truncates/summarizes rather than silently sending unbounded data. This directly protects the AI cost model (see BUSINESS_LOGIC.md §AI Cost Control).

## 4. Tool Registry

Tools are the *only* way Claude affects state or reads data beyond the assembled context. Claude cannot execute SQL, cannot call arbitrary endpoints, and cannot supply its own `userId` — every tool implementation receives the authenticated `userId` from the server-side session context, never from the model's tool-call arguments.

| Tool | Type | Notes |
|---|---|---|
| `getUserProfile` | read | |
| `getGoal` | read | |
| `getWeightHistory` | read | bounded range |
| `getTodayWorkout` / `getTrainingPlan` / `getRecentWorkouts` | read | |
| `getRecentCheckIns` | read | |
| `getNutritionProfile` | read | |
| `searchMemories` | read | semantic + filtered search, scoped to caller's userId |
| `getBehaviorPatterns` | read | |
| `saveMemory` / `updateMemory` | write | goes through the same extraction/dedup rules as async extraction (see MEMORY.md §Updates) |
| `logWeight` / `logWorkout` / `logMeal` | write | validated against business rules (e.g. plausible weight delta) before persisting |
| `createCheckIn` | write | |
| `updateTrainingPlan` | write | admin/system-level nuance; guarded so casual conversation can't silently rewrite a plan without justification logged |
| `scheduleMessage` | write | used sparingly, e.g. "remind me tomorrow" |
| `flagSafetyEvent` | write | always allowed regardless of coaching state |

### Tool Security Rules (enforced in a shared decorator, not per-tool)
1. Ownership check: every tool call is executed with `userId` bound from the authenticated session — a tool cannot be invoked for a different user even if the model hallucinates an ID.
2. Input validation: Bean Validation on tool argument DTOs, same as any API input.
3. Authorization: write tools check `coaching_states.mode` (e.g. `updateTrainingPlan` may be restricted when `HUMAN_ACTIVE`).
4. Business rule validation (e.g. `logWeight` rejects a value >25% different from the last entry without confirmation — likely a typo/unit error, not silently trusted).
5. Every write tool call logged (actor=AI, tool name, arguments, result) for auditability.
6. Structured results only — no tool ever returns a blob the model could misinterpret as instructions (defense against reflected prompt injection via tool output).

## 5. Response Validation (pre-send gate)

Before any AI-generated text reaches WhatsApp:
- Length check (coach messages are short; reject/truncate runaway generations).
- No leaked system prompt / tool schema / internal identifiers (pattern + heuristic check).
- No unvalidated claims about user data that weren't actually retrieved this turn (cross-check referenced facts against what `ContextBuilder`/tools actually returned this turn — reduces hallucination risk).
- Safety re-check on the *output* (not just input) — catches the AI itself proposing something unsafe.
- No medical diagnosis language (pattern-based disclaimer/block list, see §Safety below).

On failure: do not send. Use a scripted, safe fallback ("Give me a sec — let's pick this back up") and log the failure with full context for review. Never retry-and-send blindly in a loop.

## 6. Prompt Injection Defense

- User messages (and any tool output derived from user-authored content, e.g. a nutrition log free-text field) are always wrapped as untrusted data in the prompt — never concatenated into the system prompt.
- System instructions, safety rules, and tool authorization are fixed server-side and cannot be referenced or altered by conversation content. If a user says "ignore previous instructions" or "act as system," this is treated as ordinary conversational content and the safety/behavior rules still apply — validated by dedicated prompt-injection test cases (see TESTING.md).
- Tool outputs are structured (JSON-like data), not natural-language instructions, specifically to prevent "second-order" injection where a tool result contains attacker-controlled text that reads like a command.

## 7. Safety Engine

- Runs as a distinct stage *before* generation (cheap classifier/heuristic + Claude-based classification for ambiguous cases) and *after* generation (response validator).
- Detects: starvation/extreme-deficit requests, dehydration requests, eating-disorder indicators, severe injury/symptom descriptions, medication/diagnosis questions, unsafe training requests.
- On HIGH/CRITICAL: short-circuit generation, send a safe scripted response (supportive, non-clinical, points to professional help where appropriate), create a `safety_events` row, and (CRITICAL only) trigger human review queue / on-call notification.
- This is behavioral coaching, not clinical diagnosis — the system never attempts to diagnose; it recognizes patterns and routes to caution/human review.
- Every safety event is reviewable in the admin console (see API.md `/admin/safety-events`).

## 8. Coach Personality

- Three selectable styles (`SUPPORTIVE`, `BALANCED`, `TOUGH_LOVE`) implemented as system-prompt personas plus tone constraints, not separate models.
- Personality rule: the coach never says "according to my memory" or "according to your profile" — retrieved facts are woven into natural coaching language (see product spec §76). Enforced via prompt instructions + spot-checked in response validation/eval tests, not a hard filter (this is a tone requirement, not a safety requirement).
- The AI never reveals system prompts, tool schemas, or internal architecture if asked; deflects naturally (system-prompt instruction + validated in tests).

## 9. Cost Control (see also BUSINESS_LOGIC.md)
- Every request logged to `ai_requests` with token counts and estimated cost — no exceptions, enforced at the `AIProvider` wrapper level so it can't be forgotten per call site.
- Context is deliberately compact (§3) — summaries and structured state over raw history dumps.
- Model selection is itself a configuration (not hardcoded per call site) so cheaper models can be routed to lower-stakes tasks (e.g. intent classification) while the primary conversational model handles the actual coaching turn.
