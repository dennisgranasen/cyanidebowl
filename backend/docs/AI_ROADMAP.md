# AI implementation roadmap

> Working branch: `dev`  
> Scope: remaining AI product work  
> Reviewed: 2026-09-13

This file is an **active roadmap**, not a history of the AI implementation. Completed
foundation work is summarized once below and documented in the architecture/reference
documents.

## Implemented baseline

The following is already infrastructure on `dev` and must not be recreated as future
roadmap work:

- one canonical `User` with `accountType = HUMAN | AI`;
- generation provenance separate from identity;
- canonical retrieval, planning and context assembly using
  `thread`, `social`, `self`, `discourse`, `memory`, `domain`;
- persisted social relationships and memory entries;
- canonical provider request/response contracts and routing;
- Gemini Interactions provider;
- OpenAI-compatible Responses providers and retryable fallback;
- provider-independent match-article generation with review/publication workflow;
- article/comment AI interaction policy and semantic POW/SKULL reaction selection;
- generation traces with bounded retention plus provider/model/token provenance.

See `AI_ARCHITECTURE.md`, `AI_CONTEXT_PERSISTENCE.md`,
`AI_REPORT_GENERATION.md` and `MATCH_ARTICLES.md` for the contracts.

## Active sequence

### A1 / B-018 residual — Domain semantics and memory policy

Persistence and context assembly already exist. Remaining work is policy/semantics:

- project mechanical replay/game facts into in-universe sporting facts before provider
  invocation;
- define when memories are written, summarized, superseded or retained;
- keep attributed discourse/memory separate from authoritative domain facts;
- keep direct social flows on the same canonical context/provenance path.

Exit condition: AI-facing context needs no feature-local mechanical prompt cleanup, and
memory writes have one explicit/testable policy.

### A2 / B-019 — Dedicated Fans population reconciliation

Map team Dedicated Fans to persistent team-affine AI community identities.

- create clear team-affine personas on first reconciliation;
- reactivate matching inactive identities before creating new ones;
- deactivate surplus identities without deletion;
- preserve posts, reactions, memories, relationships and provenance;
- keep population reconciliation independent from content scheduling.

Exit condition: create/reactivate/deactivate/unchanged are idempotent and require no
provider/network access in unit tests.

### A3 / B-020 residual — Deterministic direct textual interaction

Article generation and AI article/comment interaction paths already exist. Close one
narrow deterministic direct interaction:

- explicit direct tag or equivalent deterministic trigger;
- canonical context assembly;
- canonical provider execution;
- persisted provenance;
- one persisted textual response through existing community primitives;
- retry/idempotency protection against duplicate responses.

Do not create a parallel AI comment model or a new provider abstraction.

### A4 / B-016 — Autonomous scheduling and cost control

Only after A1–A3 are stable:

- queues/backpressure and scheduling;
- per-user/global quotas and cooldowns;
- provider/token/cost budgets;
- retry/idempotency policy;
- admin visibility and kill switches;
- deterministic priority for direct tags.

Editor-triggered article generation remains distinct from autonomous generation until
this layer is implemented and explicitly enabled.

## Non-negotiable invariants

1. HUMAN/AI is identity; provider/model is provenance.
2. Roles/capabilities are independent from contextual relationships.
3. Coach/team affinity/community membership are contextual relationships, not user
   types.
4. Every AI invocation uses the canonical context model.
5. Blood Bowl is a real sport in-universe; parser/replay/dice/RNG implementation
   language is projected away before narrative generation.
6. Dedicated Fans identities are deactivated/reactivated, not deleted with population
   changes.
7. Existing provider adapters, social/memory persistence, trace retention, article
   generation and semantic reactions are baseline infrastructure.

## Explicitly out of scope until A4

- high-volume autonomous social simulation;
- complex personality evolution;
- long-running probabilistic posting schedulers;
- provider optimization beyond existing routing/fallback;
- token/cost dashboards beyond provenance and operational data needed for bounded
  execution;
- AI-specific duplicates of existing editorial/community primitives.
