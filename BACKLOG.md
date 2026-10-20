# Development backlog

> Working branch: `dev`  
> Reviewed: 2026-10-20

This file contains **active actionable work only**. It is intentionally not a history of
the project. See `ROADMAP.md` for direction and `CHANGELOG.md` for completed work.

## P1 — Stage/source runtime correctness

### Nullable StageSource game

`StageSource.game` is legitimately unknown for some sources. Current match adaptation still
fails when such a source contains matches because game-specific `MatchAdapter` selection is
required too early.

- Separate raw source-match lookup/selection from game-specific adaptation.
- Ensure community/editorial audience aggregation can obtain relevant teams/players without
  requiring `game` when that information is not semantically necessary.
- Keep strict errors only at call sites that truly require a game-specific adapter.
- Add regression coverage for:
  - source without `game` and no matches;
  - source without `game` with matches;
  - community-directory/article-audience requests spanning such a source.

## P2 — Production and queue hardening

### Queue diagnostics and lifecycle

- Validate the existing queue estimates, provider cooldowns and recovery behavior against
  realistic backlog/quota conditions; add regression coverage for any observed failures.

### Dedicated Fan player-rating recovery

Dedicated Fan player-rating job state is currently in memory, so a backend restart can
lose the visible job and queued fan work.

- Persist job and per-fan work state, or provide an equivalent recoverable queue.
- Recover interrupted work idempotently and preserve provider-quota accounting.
- Add restart/recovery tests.

### Deployment/runbook

- Rehearse the documented ARM64 image rollback on the deployment host.
- Define backup retention and verify restoration of MongoDB, replay/media files and the
  pybb3 credentials volume, including the stable encryption key.

### Integration failure contracts

- Replace broad null/exception fallbacks with typed outcomes only where callers need to
  distinguish unavailable, unauthorized, rate-limited and invalid data.
- Do this incrementally at integration boundaries; do not create a parallel error model.

## P3 — Replay state and visualization

The BB3 semantic parser/timeline is an established baseline, not an active parser-correctness
project.

- Maintain representative cyanidebowl/pybb3 contract fixtures as new replay shapes are
  discovered.
- Turn newly discovered misleading interpretations into permanent regression fixtures.
- Complete BB2 semantic event decoding; add BB1 support when upstream format support is
  available.
- Define deterministic replay state transitions from normalized events.
- Build interactive replay visualization only after deterministic reconstruction is stable.

## Rules for implementation

1. Read affected code completely before changing architectural behavior.
2. Preserve the canonical LeagueSystem/Season/Phase/Stage/StageSource hierarchy.
3. Preserve game-aware coach claims and scoped administration/editorial permissions.
4. Never commit secrets, credentials, database dumps or replay IP addresses.
5. Add regression coverage for behavior changes.
6. Update the owning documentation in the same change when a documented contract changes.
7. Remove completed backlog items instead of accumulating a permanent project diary here.
