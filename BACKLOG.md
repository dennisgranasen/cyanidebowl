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
2. Preserve the existing `LeagueSystem`, `Season`, `Phase`, `Stage`, `StageSource`,
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
8. `Staff` is the canonical code/API/route term for public editorial identities.
   `Redaktion` is only the Swedish UI translation.

## Priority overview

| Priority | Item | Outcome | Status |
| --- | --- | --- | --- |
| P1 | B-025 | Versioned pybb3 timeline/skill-reroll contract is consumed safely | Integration pending upstream |
| P1 | B-033 | Replay parsing is stable and regression-tested | Backlog |
| P1 | B-032 | Human editors have editable public Staff profiles | Done |
| P1 | B-019 | Dedicated Fans population reconciliation | Done |
| P1 | B-020 | Deterministic direct AI textual interaction | Partial |
| P2 | B-028 | Legacy Circuit hierarchy retired; canonical navigation remains | Done |
| P2 | B-029 | LeagueSystem selection lives in the primary navigation | Done |
| P2 | B-030 | Match cards and immediate match modal are internationalized | Done |
| P2 | B-031 | AI reporter public profiles are improved | Done |
| P2 | B-035 | BB1 and BB2 replays use the normalized replay pipeline | Backlog |
| P2 | B-034 | Replays can be reconstructed and visualized interactively | Depends on B-033/B-035 |
| P2 | B-016 | AI scheduling, quotas and cost controls | Deferred until B-019/B-020 |
| P2 | B-027 | Raspberry Pi operational runbook is complete | Partial |
| P3 | B-012 | Broad null/exception fallbacks become typed outcomes | Blocked on policy decision |
| P3 | B-015 | Team comment streams | Blocked on canonical team identity |

## Current baseline

Already implemented:

- canonical LeagueSystem/Season/Phase/Stage/StageSource model and admin CRUD;
- LeagueSystem-first public navigation with phase-aware season structure;
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
- ARM64 CI image builds and Raspberry Pi compose deployment baseline;
- primary-navigation LeagueSystem switching with URL-addressable selected-system state;
- a shared public Staff surface containing human editors and AI reporters;
- self-editable human Staff profile fields stored separately from auth/login identity;
- public AI reporter profile type/display-name fields without public runtime/provider
  configuration;
- Staff-profile breadcrumb support and initial match-card/match-modal i18n coverage.

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

### B-033 — Stabilize replay parsing

**Status: Backlog**

Replay parsing still produces semantically odd or misleading results for some replays.
Treat those as parser/normalization defects rather than compensating for them in the UI.

Work:

- collect every known bad/odd replay as a permanent regression fixture;
- make event interpretation deterministic and preserve source ordering/provenance;
- never invent a roll, target, outcome or reason when the replay does not establish it;
- represent unknown/unsupported events explicitly rather than mapping them to a plausible
  but incorrect generic event;
- regression-test negative traits, movement tests, rerolls, kickoff events and other
  historically fragile event families;
- version parser/output changes that affect the normalized timeline contract.

Detailed BB3 replay decoding belongs upstream in `pybb3`; Cyanidebowl owns the
versioned service contract, validation and presentation of that data.

**Acceptance criteria**

- every reported parser defect has a fixture before or with the fix;
- repeated parsing of the same replay produces the same normalized result;
- ordinary movement cannot acquire a fabricated dice roll;
- unsupported data remains inspectable without being presented as a known event.

### B-035 — Add BB1 and BB2 replay support

**Status: Backlog**

BB1 and BB2 replays are not yet integrated into the current replay pipeline.

Work:

- detect replay game/edition before parsing;
- add edition-specific adapters/parsers for BB1 and BB2;
- normalize supported BB1/BB2 events into the same versioned replay contract consumed
  by Cyanidebowl;
- preserve edition-specific source/provenance where semantics differ;
- keep unsupported events explicit rather than forcing BB1/BB2 data into BB3-specific
  assumptions;
- support ingestion, storage, diagnostics and match/replay presentation for all three
  supported game generations;
- add representative BB1 and BB2 replay fixtures.

**Acceptance criteria**

- Cyanidebowl can identify and process representative BB1, BB2 and BB3 replays through
  one normalized consumer boundary;
- edition-specific parser behavior is isolated behind adapters;
- unsupported semantics degrade explicitly, not silently or incorrectly.

### B-034 — Replay state engine and visualization

**Status: Backlog — depends on a stable normalized replay contract**

Build a replay engine that can reconstruct match state from normalized replay events
and drive an interactive visualization.

Separate the deterministic state engine from the visual renderer:

- reconstruct pitch state, players, ball, possession, turn/half/drive and relevant
  transient state from the event stream;
- support step forward/back, play/pause, timeline seek/scrub and playback speed;
- make event-to-state transitions deterministic and testable without the UI;
- expose unknown/unsupported transitions instead of inventing visual state;
- keep the renderer edition-agnostic wherever the normalized contract permits it.

**Acceptance criteria**

- a replay can be reconstructed to the same state at a given timeline position whether
  reached by sequential playback or seeking;
- engine tests do not require a browser;
- visualization consumes the state engine rather than reparsing raw replay payloads.

---

## UI and navigation follow-up

### B-028 — Retire legacy Circuit navigation/domain

**Status: Done**

The superseded `Circuit -> CircuitLeg -> CircuitLegEntity` model has been removed from
the frontend and backend, including routes, controllers, services, repository, client
API calls and navigation state.

`LeagueSystem -> Season -> Phase -> Stage -> StageSource` is the canonical competition
structure. Do not reintroduce Circuit as a parallel hierarchy.

Future breadcrumb regressions should be tracked as focused defects against the canonical
routes.

### B-029 — Move LeagueSystem selection into primary navigation

**Status: Done**

LeagueSystem selection lives in the primary menu. Switching systems preserves query
state while already on `/`, replaces the `leagueSystem` query value, and otherwise
falls back deterministically to the selected LeagueSystem landing page. The behavior is
covered by focused frontend tests.

There are currently no separate nested LeagueSystem public routes. If such routes are
introduced later, extend the navigation helper rather than adding another selector.

### B-030 — Complete i18n for match cards

**Status: Done**

`ContestMatchCard` and the immediate `MatchModalWithRosters` roster/replay surface use
react-intl for user-facing labels, warnings and download text.

Regression coverage verifies that hard-coded roster/replay labels are not reintroduced
and that localization changes cannot mutate JavaScript identifiers such as
`specialEvents`, `resourceEvents`, `checkpointCount`, `eventCount`, `stepCount` and
`dieValueCounts`.

Technical Blood Bowl abbreviations such as SPP, TD and KO remain intentionally
untranslated.

---

## Editorial/community follow-up

### B-032 — Human public Staff profiles

**Status: Done**

`Staff` is the canonical internal/product domain. `Redaktion` remains only the Swedish
UI translation.

Implemented:

- HUMAN users with site-editor or LeagueSystem-editor permission resolve to one public
  Staff profile and disappear from public Staff listings if all qualifying permissions
  are removed, without deleting their user or authored history;
- public display name, avatar, portrait and biography are separate from auth/login
  identity and OAuth only seeds the public identity once;
- the synthetic development account never persists a Staff profile;
- display name falls back to the canonical username when left blank;
- public image URLs accept only absolute HTTP/HTTPS URLs, with a 2048-character limit;
- display name is single-line and limited to 80 characters; biography is limited to
  2000 characters; blank optional fields are stored as null;
- Account editing uses an explicit public-profile payload and matching client limits;
- canonical human deep links are `/staff/user/<id>` and are regression-tested;
- backend regression coverage protects eligibility, permission loss, OAuth seed-once,
  identity separation and validation behavior.

Do not add auth subject, permissions or ownership fields to the public Staff edit
payload.

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

### B-031 — Improve AI reporter public profiles

**Status: Done**

The public AI Staff profile is a projection of the canonical `AiReporterDefinition`,
not a second identity model.

Implemented:

- public DTOs expose explicit `profileType`, stable display name, race, category,
  editorial role, summary, portrait/avatar, filtered public Markdown and active state;
- display name, role and image fields have deterministic fallbacks for incomplete
  profiles;
- the short public summary is derived from the existing `Public profile` section when
  available and otherwise falls back to role/category;
- Staff cards and full reporter profiles show the existing structured role/category
  information and use image/summary fallbacks;
- provider, prompt, runtime and other internal execution configuration remain outside the
  public identity contract;
- AI and HUMAN profiles share the canonical `/staff` surface.

Future copy changes are content edits to reporter definitions, not an open architecture
feature.

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

**Status: Done**

Dedicated Fans owns the desired active population of persistent AI-backed
`COMMUNITY_MEMBER` profiles for each team.

Implemented:

- canonical identity remains `WarpScoresUser` with `AccountType.AI`;
- `AiCommunityMemberProfile` stores only community role, team affinity,
  deterministic persona seed and lifecycle state;
- active population equals the latest known non-null `Team.dedicatedFans` value;
- increases reactivate inactive profiles before creating new canonical users;
- decreases deactivate surplus profiles and never delete either profiles or users;
- reactivation preserves the original canonical user ID and therefore authored
  history, reactions, memory/relationships and provenance references;
- null/unknown Dedicated Fans values are non-destructive and skip reconciliation;
- scheduled team refreshes reconcile after refreshed team data has been persisted;
- reconciliation is deterministic/local and has no provider or network dependency.

Focused regression coverage protects create/reactivate/deactivate/idempotent behavior,
canonical user-ID preservation, non-deletion and HUMAN/AI subject collision handling.

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
- B-021 competition structure and round/presentation correctness: the
  `Season -> Phase -> Stage` hierarchy is implemented, including phase-aware public
  overview data and legacy compatibility for phase-less stages; round-robin match days
  are reconstructed deterministically, latest played round is selected by default and
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
