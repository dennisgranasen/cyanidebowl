# Development backlog

> Working branch: `dev`  
> Reviewed: 2026-09-16

This file contains **active actionable work only**. It is intentionally not a history of
the project. See `ROADMAP.md` for direction and `CHANGELOG.md` for completed work.

## P1 — Replay contract and parser correctness

### Replay timeline contract

- Pin and validate the pybb3 replay-analysis/timeline contract at the service boundary.
- Add representative consumer fixtures for known replay shapes.
- Render roll/result only when an actual test occurred and preserve the semantic reason.
- Consume `skill_rerolls` distinctly from generic/team rerolls.
- Preserve explicit unknown-event provenance.
- Keep replay IP redaction covered by regression tests.

### Replay parser regressions

- Turn every known misleading replay interpretation into a permanent regression fixture.
- Fix defects in pybb3/normalization rather than compensating for parser mistakes in UI.
- Keep backend parser-version handling synchronized with the normalized pybb3 response.
- Verify failed current-version analyses do not enter an infinite retry loop.

### Replay state and visualization

- Define deterministic replay state transitions from normalized events.
- Add BB1/BB2 adapters behind the same normalized model when upstream support is ready.
- Build interactive visualization only after deterministic reconstruction is stable.

## P2 — Production and queue hardening

### Queue diagnostics

- Validate the admin diagnostics under realistic backlog/quota conditions.
- Persist model-execution throughput history if restart-surviving history proves useful;
  current provider execution queues are in-memory.
- Keep deterministic replay analysis visually and operationally separate from AI queues.
- Prefer explicit retry/resume reasons over opaque “pending” states.

### Deployment/runbook

- Document image upgrade and rollback on Raspberry Pi/ARM64.
- Document persistent volumes (`replay_data`, `community_media`, pybb3 credentials) and
  backup/restore expectations.
- Document Cloudflare Tunnel diagnosis and container health/start ordering.
- Keep secrets host-local and keep build-time public frontend configuration distinct from
  runtime secrets.

### Integration failure contracts

- Replace broad null/exception fallbacks with typed outcomes where the caller needs to
  distinguish unavailable, unauthorized, rate-limited and invalid data.
- Do this incrementally at integration boundaries; do not create a parallel error model.

## P2 — Editorial/community follow-up

- Reuse the existing article/review/provenance model for broader editorial article flows.
- Keep AI-authored articles reviewable, with automatic acceptance only when explicitly
  configured.
- Keep image generation renderer-neutral from the editorial/domain model.
- Add autonomous content producers only for explicit domain events approved by product
  behavior.
- Replace fixed provider cooldowns with provider-aware retry/backoff when normalized error
  metadata supports it reliably.

## Rules for implementation

1. Read affected code completely before changing architectural behavior.
2. Preserve the canonical LeagueSystem/Season/Phase/Stage/StageSource hierarchy.
3. Preserve game-aware coach claims and scoped administration/editorial permissions.
4. Never commit secrets, credentials, database dumps or replay IP addresses.
5. Add regression coverage for behavior changes.
6. Update the owning documentation in the same change when a documented contract changes.
7. Remove completed backlog items instead of accumulating a permanent project diary here.
