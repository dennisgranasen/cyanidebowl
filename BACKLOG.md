# Development backlog

> Handoff target: Codex  
> Working branch: `dev`  
> Reviewed: 2026-09-13  
> Repository: `dennisgranasen/cyanidebowl`

`ROADMAP.md` defines execution order. This document contains the detailed work cards
that are still actionable, followed by a compact record of implemented foundations.

Do not use old `main` behavior as the basis for implementation work.

## Rules for Codex

1. Read every affected file completely before editing it.
2. Preserve the existing `LeagueSystem`, `Season`, `Stage`, `StageSource`,
   `MatchInterpretation`, archive-provider and match-adapter architecture.
3. Preserve the canonical authorization model: Auth0 subject identifies the user,
   `coachClaims` are game-aware coach identities, `siteAdmin` is the super-role and
   LeagueSystem administration/editorial permissions may be scoped.
4. Never commit `.env`, database credentials, Auth0 tokens, Cyanide API keys, Steam
   credentials, database dumps, replay IP addresses or generated secrets.
5. Keep Cyanide-disabled local development as the safe default.
6. Add regression coverage for behavior changes.
7. If product behavior is genuinely ambiguous, document the decision point instead of
   inventing a second competing model.

## Priority overview

| Priority | Item | Outcome | Status |
| --- | --- | --- | --- |
| P1 | B-025 | Versioned pybb3 timeline/skill-reroll contract is consumed safely | Integration pending upstream |
| P1 | B-019 | Dedicated Fans population reconciliation | Backlog |
| P1 | B-020 | Deterministic direct AI textual interaction | Partial |
| P2 | B-016 | AI scheduling, quotas and cost controls | Deferred until B-019/B-020 |
| P2 | B-027 | Raspberry Pi operational runbook is complete | Partial |
| P3 | B-012 | Broad null/exception fallbacks become typed outcomes | Blocked on policy decision |
| P3 | B-015 | Team comment streams | Blocked on canonical team identity |

## Current baseline

Already implemented:

- canonical LeagueSystem/Season/Stage/StageSource model and admin CRUD;
- LeagueSystem-first public navigation;
- deterministic round-robin match-day reconstruction, independent current rounds and
  latest-played-round selection in the LeagueSystem UI;
- standings and playoff bracket presentation, including play-in/QF/SF/final/bronze
  inference and replay/rematch series;
- interactive Cyanide discovery, LeagueSystem alias-based candidate discovery and
  explicit StageSource registration;
- historical match-detail backfill with retryable availability state and canonical race
  presentation;
- stage match APIs and normalized match presentation;
- canonical game-aware authorization and scoped administration/editorial permissions;
- editorial/community articles, comments, reactions and player ratings;
- canonical AI identity, context assembly, provider abstraction and provenance;
- Gemini plus OpenAI-compatible execution/fallback;
- persisted AI social relationships and memory entries;
- AI article generation/review, interaction decisions, semantic reactions and
  generation traces;
- ARM64 CI image builds and Raspberry Pi compose deployment baseline.

Do not reopen those capabilities as broad feature cards. New defects should be tracked
as focused deltas.

---

## Active core product work

### B-025 — Consume the versioned pybb3 timeline contract

**Status: Integration pending upstream contract**

Detailed replay parsing belongs to the separate `pybb3` repository. Cyanidebowl work
starts at the service/consumer boundary.

Required upstream semantics include:

- ordered half/drive/turn events;
- actor/target plus semantic reason for an actual test/roll;
- no fabricated roll for ordinary movement;
- explicit dodge, rush/GFI, Tentacles and negative-trait checks;
- explicit kickoff deviation, wizard, Bone-head, Really Stupid, Bloodlust,
  Foul Appearance and Animal Savagery events where applicable;
- `skill_rerolls` distinct from generic/team rerolls with compatibility for older
  payloads;
- explicit unknown-event provenance instead of misleading generic mappings.

Cyanidebowl tasks:

- pin an agreed upstream pybb3 ref;
- version/validate the timeline contract in `pybb3-service`;
- add representative consumer fixtures/tests;
- render reason/roll/result only when semantically applicable;
- preserve replay IP redaction in diagnostic/export paths.

---

## Editorial/community follow-up

### B-015 — Enable team comment streams

**Status: Blocked**

The generic community target model already contains TEAM. Do not implement a second
team-comment model.

Unblock when:

- one canonical historical/current team identity is stable;
- the team endpoint reliably resolves that identity;
- LeagueSystem ownership/scope can be resolved for the team.

Then:

- enable TEAM in the existing editorial/community service;
- render the existing comment thread component on the team page;
- reuse scoped-editor/site-admin moderation rules.

---

## AI work

### B-018 residual — Domain semantic projection and memory-write policy

**Status: Partial foundation**

Already implemented:

- canonical context envelope;
- deterministic retrieval/planning/assembly;
- social relationship persistence;
- memory persistence/retrieval;
- canonical provider requests and provenance.

Remaining:

- translate mechanical replay/game data into in-universe sporting facts before provider
  invocation;
- define when memories are written, summarized, superseded or retained;
- prevent attributed discourse/memory from being promoted to authoritative domain facts;
- keep direct social flows on the canonical context/provenance path.

### B-019 — Dedicated Fans community population reconciliation

**Status: Backlog — prerequisite for B-016**

- desired active AI-backed `COMMUNITY_MEMBER` population equals team Dedicated Fans
  value;
- first reconciliation creates clear team-affine personas;
- increases reactivate inactive matching identities before creating new identities;
- decreases deactivate surplus identities without deletion;
- preserve authored history, reactions, memory, relationships and provenance.

**Acceptance criteria**

- supports create/reactivate/deactivate/unchanged;
- unchanged input is idempotent;
- reactivation preserves canonical user identity;
- reconciliation tests require no provider, scheduler or network access.

### B-020 — Deterministic direct AI textual interaction

**Status: Partial — article generation and reaction/interaction paths exist**

Close one narrow deterministic textual flow before autonomous scheduling:

- enabled AI user;
- supported target/thread;
- explicit direct tag or equivalent deterministic trigger;
- canonical context assembly;
- canonical provider execution;
- persisted generation provenance;
- persisted textual response through existing community primitives;
- retry/idempotency protection against duplicate responses.

Do not add a parallel AI comment model or another provider abstraction.

### B-016 — AI scheduling, quotas and cost control

**Status: Deferred until B-019/B-020 are stable**

Owns operational policy:

- scheduling and queue/backpressure;
- per-AI-user/global generation quotas;
- provider/token/cost budgets;
- cooldowns, retries and idempotency;
- admin visibility and kill switches;
- priority handling for direct tags.

Editor-triggered article generation remains distinct from autonomous generation until
this layer is implemented and explicitly enabled.

---

## Operational/maintainability work

### B-027 — Finish ARM64/Raspberry Pi operational runbook

**Status: Partial — build/deploy baseline exists**

Document remaining operational knowledge:

- loading/versioning the application images;
- `.env`/secret ownership and required variables;
- `PYBB3_REF`, internal API key and credential-encryption key responsibilities;
- persistent replay/credential volumes;
- health/startup ordering;
- Cloudflare Tunnel failure diagnosis;
- upgrade/rollback and cleanup of local tar artifacts.

Do not describe ARM64 build support or the Raspberry Pi compose topology as future
implementation work.

### B-012 — Replace broad exception/null fallbacks with typed outcomes

**Status: Blocked on failure-policy decision**

Do this one service/controller at a time.

First decide how scheduled collection distinguishes:

- not found;
- invalid input;
- upstream unavailable;
- rate limited;
- internal client failure.

In particular, decide whether each `CyanideRestApiClient` outcome causes skip, retry or
job failure before replacing the existing ambiguous null/error contract.

Never expose stack traces, secrets, sensitive URLs or full upstream payloads.

---

## Completed foundations — compact history

The following backlog areas are considered implemented and should not dominate future
handoff documents:

- B-001 contest-fetch candidate collection fix;
- B-002 public debug-header removal;
- B-003/B-004 HTTP security alignment and auth regression coverage;
- B-005 frontend permission-state refactor foundation;
- B-006 stage aggregation hardening;
- B-007 stage read/admin application boundary;
- B-008 frontend API cleanup;
- B-009 profile/auth documentation alignment;
- B-010 CI and ARM64 artifact build foundation;
- B-011 repository metadata/docs cleanup;
- B-013 interactive Cyanide discovery and StageSource registration;
- B-014 editorial/community foundation;
- B-017 canonical Human/AI user identity and provenance;
- canonical AI retrieval/planning/provider foundations AI-003 through AI-008;
- B-021 competition round/presentation correctness: round-robin match days are
  reconstructed deterministically, latest played round is selected by default and
  regression coverage includes postponed matches that must not create extra rounds;
- B-022 standings/playoff series correctness: standings, play-in/QF/SF/final/bronze
  bracket inference, duplicate handling and replay/rematch series are implemented with
  regression coverage;
- B-023 discovery candidate flow: interactive discovery plus LeagueSystem alias-based
  candidate discovery and explicit StageSource registration are implemented. A future
  external polling/watch service is not an active requirement; add it as a new focused
  enhancement only when there is a concrete product need;
- B-024 historical match-detail enrichment: retryable backfill/status handling and
  canonical race presentation are implemented. Remaining bad historical records are
  data-specific defects, not an open feature card;
- primary LeagueSystem-first public navigation.

If a regression is found in one of these areas, create a focused defect/fix rather than
restoring the old broad implementation card.

## Verification

### Backend

```bash
mvn clean test -Pserver -DskipDocker -pl api,cyanide-api,backend -am
mvn clean package -Pserver -DskipDocker -pl api,cyanide-api,backend -am
```

### Frontend

```bash
cd frontend
npm ci
npm test -- --runInBand
npm run build
```

### Safety

- `git status --short` contains no `.env`, dumps, tokens or generated artifacts.
- Changed logs contain no API keys, Authorization headers, cookies, MongoDB URIs or
  full upstream response bodies.
- Server tests require no live network access.

## Definition of done

- automated tests cover changed behavior;
- relevant commands pass;
- configuration/public behavior changes are documented;
- no secrets or generated artifacts are committed;
- the commit is focused and references the backlog item;
- roadmap/backlog status is updated in the same change.
