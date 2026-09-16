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
- canonical HUMAN/AI identity, AI context/provenance and provider abstraction;
- durable autonomous AI work and community-media queues;
- deterministic replay download/storage/reanalysis separated from LLM execution;
- authenticated `pybb3-service` boundary and versioned replay-analysis contract;
- ARM64/Raspberry Pi Compose deployment behind Cloudflare Tunnel.

Do not reopen those foundations as green-field roadmap items.

## R1 — Replay correctness and visualization

The immediate replay priority is correctness rather than more presentation features.

- Keep the cyanidebowl/pybb3 contract versioned and covered by fixtures.
- Stabilize semantic timeline output: rolls appear only for actual tests and include the
  reason for the test.
- Consume skill rerolls separately from team/generic rerolls while remaining compatible
  with older replay payloads where required.
- Preserve unknown-event provenance and replay IP redaction.
- Add BB1/BB2 adapters behind the same normalized consumer model when upstream support is
  ready.
- Build deterministic replay state reconstruction before interactive visualization.

The backend owns orchestration, compatibility and presentation. `pybb3` owns raw BB3
replay decoding/parsing.

## R2 — Production hardening

Reduce operational knowledge that currently lives only with the maintainer.

- Keep build/test paths hermetic enough that normal verification does not require live
  external services.
- Document and verify upgrade/rollback for ARM64 images and Compose.
- Make persistent-volume ownership and backup expectations explicit.
- Improve startup/health diagnostics for backend, pybb3 and Cloudflare Tunnel.
- Replace ambiguous integration null/error handling with typed outcomes where practical.
- Keep queue diagnostics useful: backlog size, retry/resume state, throughput and failures.

## R3 — Editorial/community evolution

Continue editorial and community work through the existing primitives rather than adding
parallel systems.

- General articles, match reports and AI-authored material should share the same
  publication/review/provenance model where their semantics overlap.
- AI-generated content must remain reviewable or explicitly configured for automatic
  acceptance.
- Community/Fan generation should remain durable and recoverable across restarts.
- Add autonomous candidate producers only for concrete product events; avoid generic
  background content polling.

## Cross-cutting invariants

- `dev` is the integration branch.
- Competition behavior is metadata/configuration driven, never tournament-name hardcoded.
- `Staff` is the canonical editorial identity term in code/API/routes.
- HUMAN/AI is identity; provider/model is provenance.
- Replay analysis is deterministic and independent of LLM quotas.
- Successful replay parser version and latest attempted parser version are distinct facts.
- Secrets, Steam credentials, Auth0 tokens, MongoDB URIs and replay IP addresses must not
  be committed or emitted in diagnostics.
- New behavior receives regression coverage.
