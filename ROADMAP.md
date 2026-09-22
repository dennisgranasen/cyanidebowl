# BlaskScore roadmap

> Working branch: `dev`  
> Reviewed: 2026-09-16

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
- quota-scoped text execution queues with provider-aware retry/resume handling;
- Cloudflare community-image quota handling that waits for free quota reset rather than
  accumulating paid usage;
- deterministic replay download/storage/reanalysis separated from LLM execution;
- BB3 semantic replay timeline/narrative parsing with checks, rerolls, unknown-event
  provenance and IP redaction;
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
- Document and verify ARM64 image upgrade/rollback.
- Document persistent-volume backup/restore expectations for replay data, community media
  and pybb3 credentials.
- Improve startup/health diagnosis for backend, pybb3 and Cloudflare Tunnel.
- Replace ambiguous integration null/error handling with typed outcomes where callers
  genuinely need to distinguish unavailable, unauthorized, rate-limited and invalid data.

## R2 — Editorial/community product evolution

Continue through the existing article/community primitives rather than adding parallel
systems.

- Keep general articles, match reports and AI-authored material on the same
  publication/review/provenance model where their semantics overlap.
- AI-generated content remains reviewable unless automatic acceptance is explicitly
  configured.
- Community/Fan generation remains durable and recoverable across restarts.
- Add autonomous candidate producers only for concrete product/domain events; avoid
  generic background content polling.
- Keep image generation renderer-neutral in the domain model and cost-safe: automatic
  fallback must never introduce paid usage unless explicitly enabled by configuration.

## R3 — Replay state and visualization

The BB3 parser/timeline itself is no longer the primary replay project. Future replay work
is product functionality above the normalized semantic event stream.

- Maintain the cyanidebowl/pybb3 contract with representative fixtures and regression
  coverage as parser knowledge evolves.
- Build deterministic replay state reconstruction from normalized events.
- Build interactive replay visualization only after deterministic reconstruction is
  stable.
- Add BB1/BB2 adapters behind the same normalized consumer model when upstream support is
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
