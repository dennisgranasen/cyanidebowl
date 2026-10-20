# BlaskScore roadmap

> Working branch: `dev`  
> Reviewed: 2026-10-20

This file contains broad remaining direction only. Detailed implementation tasks belong
in `BACKLOG.md`. Completed work belongs in `CHANGELOG.md`.

## Current baseline

BlaskScore already has the foundations required for continued product work:

- canonical `LeagueSystem -> Season -> Phase -> Stage -> StageSource` competition model;
- LeagueSystem-first public navigation, standings, group rounds and playoff brackets;
- Cyanide discovery/source registration and historical match enrichment;
- articles, comments, reactions, ratings and public Staff/community surfaces;
- human and AI-authored article workflows with review, provenance and configurable auto-accept;
- canonical HUMAN/AI identity, AI context/provenance and provider abstraction;
- durable autonomous AI work and community-media queues;
- provider-quota AI queues with retry/resume and queue diagnostics;
- replay-grounded AI Staff and Dedicated Fan player ratings;
- provider-routed community-image generation with Cloudflare quota handling that waits
  for free quota reset rather than accumulating paid usage;
- deterministic replay download/storage/reanalysis separated from LLM execution;
- BB3 semantic replay timeline/narrative parsing with checks, rerolls, unknown-event
  provenance and IP redaction;
- BB2 archive extraction and raw analysis, with semantic decoding still incomplete;
- authenticated `pybb3-service` boundary and versioned replay-analysis contract;
- ARM64/Raspberry Pi Compose deployment behind Cloudflare Tunnel.

Do not reopen those foundations as green-field roadmap items.

## R1 — Runtime correctness and production hardening

The immediate priority is eliminating remaining runtime edge cases and making operation
recoverable without maintainer-specific knowledge.

- Make `StageSource` consumers correct when `game` is legitimately unknown. Match/source
  discovery must not make unrelated community/editorial pages fail merely because a source
  is not yet game-classified.
- Keep queue diagnostics accurate under real quota/backlog conditions, including
  retry/resume reasons, cooldown countdowns, throughput and stale-job recovery.
- Keep build/test paths hermetic enough that normal verification does not require live
  external services.
- Rehearse the documented ARM64 image rollback and test persistent-volume restoration on
  the deployment target.
- Replace ambiguous integration null/error handling with typed outcomes where callers
  genuinely need to distinguish unavailable, unauthorized, rate-limited and invalid data.

## R2 — Editorial/community product evolution

The shared article, community and image foundations are implemented. Extend those
primitives rather than creating parallel systems. Keep AI publication reviewable unless
automatic acceptance is explicitly configured, add producers only for concrete domain
events, and never switch a free-provider failure to paid generation automatically.

The remaining reliability gap is restart recovery for Dedicated Fan player-rating jobs;
their job state is currently held in memory.

## R3 — Replay state and visualization

The BB3 parser/timeline itself is no longer the primary replay project. Future replay work
is product functionality above the normalized semantic event stream.

- Maintain the cyanidebowl/pybb3 contract with representative fixtures and regression
  coverage as parser knowledge evolves.
- Build deterministic replay state reconstruction from normalized events.
- Build interactive replay visualization only after deterministic reconstruction is
  stable.
- Complete BB2 semantic event decoding (archive extraction and raw analysis exist, but
  canonical action/timeline output does not); add BB1 decoding when upstream support is
  available.

## Cross-cutting invariants

- `dev` is the integration branch.
- Competition behavior is metadata/configuration driven, never tournament-name hardcoded.
- `Staff` is the canonical editorial identity term in code/API/routes.
- HUMAN/AI is identity; provider/model is provenance.
- AI text scheduling is scoped to the actual configured quota resource, not artificially
  separated by target.
- Replay analysis is deterministic and independent of LLM quotas.
- Successful replay parser version and latest attempted parser version are distinct facts.
- Secrets, Steam credentials, Auth0 tokens, MongoDB URIs and replay IP addresses must not
  be committed or emitted in diagnostics.
- New behavior receives regression coverage.
