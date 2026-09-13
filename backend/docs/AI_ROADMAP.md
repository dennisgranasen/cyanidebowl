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

## Parallel profile work — B-031

AI reporter public profiles need a dedicated editorial-quality pass. This work is
independent of the generation sequence below and should not block A1-A4.

- improve biography/profile copy and structured public information for each reporter;
- make personality, editorial role, affiliations and voice visible in the public
  profile;
- keep portrait/avatar and profile metadata consistent across Redaktion and authored
  content;
- keep public profile identity separate from provider/model/prompt/runtime
  configuration;
- provide graceful fallbacks for incomplete profile metadata.

This work must reuse the canonical AI `User` identity rather than introducing a second
reporter identity model.

## Active sequence

### Completed sequence — B-018/B-019/B-020 and B-016 foundation

The earlier A1-A4 dependency chain is no longer the active execution plan.

Implemented foundations now include:

- canonical domain/context semantics and memory persistence/policy seams;
- Dedicated Fans population reconciliation and durable canonical identities;
- AI-generated Dedicated Fan profiles with roster-constrained species, locale-aware
  human-facing profile text and stable visual identity;
- deterministic direct textual interaction through the canonical community path;
- generation admission/budgets, per-reporter autonomous activity policy, initiative
  policy and durable autonomous work queue;
- finalized-match fan candidates and general-article Staff comment candidates;
- durable community media queue with Cloudflare Workers AI as the default renderer.

### Current focused follow-up

1. **Dedicated Fan operational verification**
   - keep AI profile generation fail-closed: provider failures remain queued for retry
     and never create deterministic fallback identities;
   - keep startup/manual semantics explicit: full fan reconciliation begins from Site
     Admin queue/reset actions, while already queued jobs remain durable across restart;
   - verify profile/avatar consistency and Community Fans admin navigation end to end.

2. **Rate-limit/backpressure follow-up**
   - current Dedicated Fan `RATE_LIMIT` handling uses a configurable fixed global worker
     cooldown as an operational stopgap;
   - replace it later with provider-aware exponential backoff with jitter;
   - distinguish transient rate limiting from quota exhaustion when normalized provider
     error metadata makes that distinction reliable;
   - avoid provider-specific retry policy leaking into canonical identity/profile models.

3. **B-016 incremental expansion**
   - add autonomous candidate producers only for concrete domain events approved by the
     product;
   - do not invent topic discovery or generic polling;
   - add monetary cost accounting only after explicit/versioned provider-model pricing
     metadata exists.

### Next broad work

After the focused AI/community verification above, broad feature work should return to
the replay platform (`B-025`, `B-033`, `B-035`, then `B-034`) and production hardening.
Do not recreate completed provider, context, community or scheduling foundations.

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
