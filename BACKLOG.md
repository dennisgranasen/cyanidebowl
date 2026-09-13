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
| P1 | B-032 | Human editors have editable public Staff profiles | Partial — baseline implemented |
| P1 | B-019 | Dedicated Fans population reconciliation | Backlog |
| P1 | B-020 | Deterministic direct AI textual interaction | Partial |
| P2 | B-028 | Navigation hierarchy and back paths are consistent | Partial — Staff hierarchy implemented |
| P2 | B-029 | LeagueSystem selection lives in the primary navigation | Partial — primary selector implemented |
| P2 | B-030 | Match cards are fully internationalized | Partial — core card/modal surface localized |
| P2 | B-031 | AI reporter public profiles are improved | Partial — public identity DTO normalized |
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

### B-028 — Audit and normalize navigation hierarchy

**Status: Partial — Staff profile hierarchy implemented**

Implemented in R2:

- `/staff` is the canonical public parent for both human Staff profiles and AI reporter
  profiles;
- Staff profile routes expose the Staff parent in `Navigation`;
- direct Staff-profile deep links no longer depend on browser Back to recover the
  editorial hierarchy.

Remaining:

- audit the other nested public/admin/editorial routes for missing or inconsistent
  parent navigation;
- define and apply one breadcrumb/back-path convention across equivalent page types;
- verify desktop/mobile and keyboard/screen-reader behavior.

**Acceptance criteria**

- every audited nested page has a deterministic parent path;
- equivalent page types use the same navigation convention;
- direct deep links still show a valid hierarchy without navigation history.

### B-029 — Move LeagueSystem selection into primary navigation

**Status: Partial — primary-navigation selector implemented**

Implemented in R2:

- LeagueSystem choices are exposed in the primary application menu;
- the old selector-specific control on the LeagueSystem page is removed;
- selected LeagueSystem can be addressed with the `leagueSystem` query parameter;
- the selected system is visually identifiable in the menu.

Remaining:

- when switching from a nested LeagueSystem route, preserve the equivalent sub-route
  where one exists;
- otherwise use the selected LeagueSystem landing page deterministically;
- add focused responsive/deep-link regression coverage.

### B-030 — Complete i18n for match cards

**Status: Partial — core match-card and immediate modal surface localized**

Implemented in R2:

- `ContestMatchCard` user-visible started/live/duration/details labels use react-intl;
- the main `MatchModalWithRosters` tab/stat/roster/replay labels covered by R2 use
  react-intl;
- English and Swedish keys exist for the R2 surface.

Remaining:

- inventory the rest of the immediate replay/match-detail UI for hard-coded labels,
  tooltips and accessibility strings;
- use locale-aware formatting consistently where presentation still bypasses i18n;
- add regression coverage that catches hard-coded UI strings and, specifically, prevents
  translation replacement from mutating JavaScript identifiers.

Do not use global raw-string replacement for short words such as `Events` or `Count`;
they also occur inside identifiers such as `specialEvents` and `checkpointCount`.

---

## Editorial/community follow-up

### B-032 — Human public Staff profiles

**Status: Partial — functional baseline implemented**

`Staff` is the canonical internal/product domain. `Redaktion` is its Swedish UI
translation only; do not create `RedaktionController`, `/redaktion/*`, `RedaktionApi`
or a second persistence model.

Implemented in R2:

- eligible HUMAN users are exposed through the public `/staff/users` API alongside the
  existing AI reporter Staff surface;
- current eligibility is based on `siteEditor` or non-empty
  `editorForLeagueSystems`;
- public display name, avatar URL, portrait URL and bio are stored separately from
  authentication/login identity;
- the initial profile is seeded from available OAuth name/picture data once, then local
  public-profile edits are preserved;
- an authenticated eligible user can edit their own public Staff profile from Account;
- public human profiles and AI profiles are presented together on `/staff`;
- loss of current eligibility hides the human profile from the public Staff API without
  deleting the underlying user or authored history.

Remaining/hardening:

- explicitly document and test development-account behavior;
- add backend tests for site-editor-only, LeagueSystem-editor-only, combined-editor and
  permission-loss cases;
- verify whether any non-editor Staff capability should also qualify a HUMAN user; do
  not invent a parallel role to solve this;
- validate URL/public-bio input policy and fallback behavior;
- add frontend tests for own-profile editing and human profile deep links.

**Acceptance criteria**

- qualifying HUMAN users resolve to exactly one canonical Staff profile;
- public-profile edits cannot change auth subject, authorization or ownership;
- OAuth refresh/sign-in never overwrites an initialized local public profile;
- removing all qualifying editorial permissions removes the profile from public Staff
  listings without deleting authored history;
- development behavior is explicit and covered by tests.

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

**Status: Partial — public identity contract normalized**

Implemented in R2:

- public AI reporter DTOs expose explicit `profileType` and `displayName`;
- AI and HUMAN profiles can be composed on the same `/staff` public surface;
- public profile identity remains separate from provider/prompt/runtime configuration;
- Staff routing/presentation no longer requires a parallel Redaktion domain.

Remaining:

- improve biography/profile copy and structured public profile information for each
  reporter;
- make personality, editorial role, affiliations and voice apparent without exposing
  internal prompt/provider implementation details;
- keep portrait/avatar and profile metadata consistent across Staff, articles and other
  author surfaces;
- define/finalize which fields are canonical public identity versus AI persona/runtime
  configuration;
- add sensible fallbacks so incomplete AI profiles do not produce broken Staff pages.

This is profile/editorial presentation work. It must not create a second AI identity
model or couple public identity to a specific LLM provider.

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
