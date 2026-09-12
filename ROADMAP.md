# BlaskScore project roadmap

> Working branch: `dev`  
> Reviewed: 2026-09-13  
> Detailed work items: `BACKLOG.md`  
> AI-specific roadmap: `backend/docs/AI_ROADMAP.md`

This file is the short, authoritative view of **where the project is now and what should
happen next**. `BACKLOG.md` contains implementation detail and acceptance criteria; it
must not be used as a chronological reading list.

## Product direction

BlaskScore is moving from the original competition-centric warp-scores model to a
LeagueSystem-first Blood Bowl platform with four cooperating areas:

1. **Competition structure** — LeagueSystem, seasons, phases/stages, sources, rounds,
   standings and playoff brackets.
2. **Match intelligence** — normalized match detail, replay enrichment and a reliable
   BB3 event/timeline contract.
3. **Editorial/community** — articles, match reports, comments, reactions, player
   ratings and scoped editorial permissions.
4. **AI participants** — canonical AI users/reporters operating through the same
   editorial/community primitives with deterministic context, provenance and explicit
   operational controls.

The architecture should keep provider/filesystem/API-specific details at integration
boundaries. Product/domain services consume normalized concepts rather than Cyanide,
archive, replay or pybb3 implementation details.

## Current baseline

The following foundations are already present on `dev` and should be preserved:

- canonical `LeagueSystem -> Season -> Stage -> StageSource` models and read/admin APIs;
- admin-assisted Cyanide league/competition discovery and explicit source registration;
- LeagueSystem-first home-page structure with season/stage results;
- canonical game-aware `coachClaims`, `siteAdmin`, scoped LeagueSystem administration
  and separate scoped editorial grants;
- editorial/community primitives for articles, comments, reactions and player ratings;
- match articles with editor review and replay-analysis prerequisite for AI generation;
- canonical AI user/context/provider contracts, Gemini and OpenAI-compatible provider
  paths, fallback execution, social/memory persistence and generation traces;
- ARM64 Docker build workflow and Raspberry Pi deployment composition;
- pybb3-service as the authenticated internal bridge to the separately versioned pybb3
  client.

Do not re-open these foundations merely because older backlog text describes their
original implementation work.

## Execution order

### R1 — Finish LeagueSystem competition UX

This is the highest-priority product slice.

- Add explicit **Phase** semantics where a season needs them (for example preseason,
  qualifier, group stage and playoffs) while retaining Stage as the concrete grouping
  inside a phase.
- Render matches **round by round**, defaulting to the latest round with played results.
  Different groups may be on different current rounds.
- Add group/league standings from the registered stage sources.
- Add metadata-driven playoff brackets, including play-ins, quarterfinals, semifinals,
  final and bronze where configured.
- Represent replay/rematch series explicitly. Match similarity is evidence for a
  rematch, not sufficient proof by itself.
- Make the configured **primary LeagueSystem** the default public focus; other systems
  remain selectable.

Exit condition: a season can be followed from phase/stage selection through rounds,
tables and playoffs without falling back to legacy competition pages.

### R2 — Complete discovery and source lifecycle

The existing admin search is an entry point, not the final discovery system.

- Normalize discovery candidates independently of provider/filesystem layout.
- Separate candidates that are immediately importable from incomplete candidates that
  require monitoring.
- Add daily/periodic watches that suggest new competitions from already registered
  leagues/coaches without enabling collection automatically.
- Keep inspection lightweight: metadata, registered teams and a small recent-match
  preview only when explicitly requested.
- Saving a source remains the action that enables normal collection.

Exit condition: administrators can discover, inspect, accept and later follow new
season/stage sources without hand-editing persistence data.

### R3 — Match detail and historical enrichment

- Ensure selected-match detail reliably contains coaches, teams, races, rosters,
  player statistics, SPP/skills and team-value information where source data permits.
- Resolve race/team-kind through canonical mappings; avoid legacy identifiers known to
  be unreliable.
- Backfill/enrich older matches asynchronously through the normal data pipeline rather
  than making overview pages perform expensive enrichment.
- Preserve pre-penalty/on-field score information where available and expose
  administrative/competition outcomes separately.

Exit condition: historical and current match modals use the same normalized detail
contract and no longer produce blank or wrongly mapped rosters for known source data.

### R4 — Stabilize the pybb3 replay/timeline contract

Cyanidebowl owns consumption and presentation; the detailed BB3 parser lives in the
separate `pybb3` repository and is pinned into `pybb3-service` with `PYBB3_REF`.

Required upstream contract:

- ordered per-turn events with actor, target, action/reason, roll when a roll actually
  exists, result and provenance;
- movement without a test must not invent a die roll;
- dodge, GFI/rush, Tentacles and negative traits such as Bone-head, Really Stupid and
  Bloodlust must explain why a roll occurred;
- kickoff deviations, wizard effects, Foul Appearance, Animal Savagery and other
  supported special events remain explicit;
- `skill_rerolls` must be represented separately from generic/team rerolls while
  retaining compatibility for older payloads;
- replay redaction must remain safe before persisted/exported diagnostic artifacts.

Cyanidebowl should version the consumed timeline schema and degrade explicitly on
unknown events instead of guessing.

Exit condition: the match timeline can be rendered without misleading generic `Move`
rolls and without parser-specific knowledge leaking into presentation code.

### R5 — Finish AI foundation before autonomous scheduling

The AI provider layer is no longer the blocking foundation. The next AI work is:

- complete semantic projection of mechanical source data into in-universe domain facts;
- define automatic memory-write/summarization policy on top of the existing memory
  persistence seam;
- implement B-019 Dedicated Fans population reconciliation;
- finish one deterministic end-to-end direct interaction path through canonical context,
  generation provenance and existing community primitives;
- then implement B-016 scheduling, quotas, budgets, retries, kill switches and priority
  handling.

Automatic article generation policy belongs to B-016. Until that operational layer is
implemented and enabled, editor-triggered generation must remain clearly distinct from
future autonomous generation.

### R6 — Operational hardening

- Keep server-profile tests hermetic and CI green.
- Replace ambiguous null/broad-exception contracts incrementally with typed outcomes.
- Keep Raspberry Pi/ARM64 deployment supported and configuration-driven.
- Document/verify Cloudflare Tunnel and host-local secret handling without putting
  runtime credentials into GitHub.
- Remove temporary migration/patch artifacts once their changes are incorporated.

## Cross-cutting invariants

- `dev` is the integration branch; do not base active work on stale `main` behavior.
- No secret, Steam credential, API key, Auth0 token, MongoDB URI or replay IP address is
  committed or emitted in diagnostics.
- LeagueSystem/source discovery does not implicitly enable data collection.
- Phase/Stage/bracket behavior is metadata/configuration-driven, not tournament-name
  hardcoding.
- AI-backed authors are canonical users; provider/model is provenance, not identity.
- Blood Bowl AI prose is in-universe: replay/parser/dice/RNG implementation terminology
  must be projected to sporting facts before generation.
- New behavior gets regression tests and updates the relevant roadmap/backlog status in
  the same change.

## Documentation ownership

| Document | Purpose |
| --- | --- |
| `ROADMAP.md` | Current product sequence, dependencies and major exit conditions |
| `BACKLOG.md` | Detailed work items, implementation notes and acceptance criteria |
| `README.md` | Repository entry point, development/verification and doc navigation |
| `CHANGELOG.md` | User/developer-visible changes already implemented |
| `backend/docs/AI_ROADMAP.md` | Detailed AI execution order |
| `backend/docs/AI_ARCHITECTURE.md` | Canonical AI design contract/invariants |
| `backend/docs/MATCH_ARTICLES.md` | Match-article authorization/lifecycle/API |
| `backend/docs/AI_REPORT_GENERATION.md` | Current vs target AI report-generation behavior |
| `pybb3-service/README.md` | Cyanidebowl-to-pybb3 service boundary and version pinning |

When implementation changes invalidate one of these contracts, update that document in
the same patch rather than adding another competing roadmap.
