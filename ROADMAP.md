# BlaskScore project roadmap

> Working branch: `dev`  
> Reviewed: 2026-09-13  
> Detailed work items: `BACKLOG.md`  
> AI-specific roadmap: `backend/docs/AI_ROADMAP.md`

This roadmap contains **remaining product work only**. Implemented foundations belong
in architecture/reference documentation and `CHANGELOG.md`, not in the execution queue.

## Implemented baseline

The following capabilities are already present on `dev` and should not be recreated as
future roadmap work:

- `LeagueSystem -> Season -> Phase -> Stage -> StageSource` domain model, read APIs and admin CRUD;
- LeagueSystem-first public navigation with season/phase/stage selection;
- deterministic round/group presentation with match-day reconstruction and independent
  group progress;
- standings and playoff bracket rendering, including play-in/QF/SF/final/bronze,
  replay/rematch series and historical-topology regression coverage;
- interactive Cyanide league/competition discovery, LeagueSystem alias-based candidate
  discovery and explicit source registration;
- historical match-detail backfill with retryable availability classification and
  canonical race presentation;
- canonical game-aware `coachClaims`, `siteAdmin`, scoped LeagueSystem administration
  and scoped editorial permissions;
- articles, comments, reactions, player ratings and match-article review flows;
- canonical HUMAN/AI users and generation provenance;
- canonical AI context planning/assembly with social and memory persistence;
- Gemini and OpenAI-compatible provider execution with fallback;
- AI match-article generation, interaction policy, semantic POW/SKULL reactions and
  bounded generation traces;
- ARM64 image builds, Raspberry Pi compose deployment and Cloudflare Tunnel deployment
  topology;
- authenticated `pybb3-service` boundary to the separately versioned pybb3 client.

Completed capability may remain documented in `BACKLOG.md` as history, but it is not
active roadmap work.

## Active roadmap

### R1 — Replay platform

- Consume a versioned pybb3 timeline contract through `pybb3-service`.
- Render a roll only when an actual test occurred and expose the semantic reason for
  that test.
- Consume upstream `skill_rerolls` separately from team/generic rerolls while retaining
  compatibility with older payloads.
- Preserve explicit unknown-event provenance and replay IP redaction.
- Stabilize replay parsing around regression fixtures for every known odd/misleading
  interpretation.
- Add BB1 and BB2 edition-specific parsing/adapters behind the same normalized consumer
  contract used for BB3.
- Build a deterministic replay state engine, then an interactive visualization on top
  of that engine.

Detailed replay parsing belongs in the separate `pybb3` repository. Cyanidebowl owns
the service contract, validation, compatibility, state reconstruction and presentation.

Exit condition: representative BB1, BB2 and BB3 replays produce stable normalized
events; known parser defects have regression fixtures; and the normalized stream can
drive deterministic replay visualization without reparsing raw payloads.

### R2 — UI, navigation and Redaktion profiles

- Audit hierarchical navigation and provide consistent parent/breadcrumb paths; a
  writer opened through Redaktion must expose Redaktion as a parent, not only Home.
- Move LeagueSystem selection into the primary application navigation and retire the
  selector-specific hamburger/menu.
- Complete i18n coverage for match cards and their immediate child components.
- Automatically ensure a Redaktion profile for every eligible non-development
  technician/editor user. Seed available image/avatar/name from OAuth, but let the user
  override every public field, including displayed name, without changing auth identity.
- Improve AI reporter public profiles and keep their public identity distinct from
  provider/prompt/runtime configuration.

Exit condition: navigation patterns are predictable across deep links, LeagueSystem
switching is part of normal app navigation, match cards are fully localized, and human
and AI Redaktion profiles have a coherent editable/public identity model.

### R3 — Remaining AI product work

The identity/context/provider/article/reaction foundation is implemented. Continue in
this order:

1. finish semantic projection of mechanical data into in-universe domain facts;
2. define automatic memory-write/summarization policy;
3. implement B-019 Dedicated Fans population reconciliation;
4. close B-020 with one deterministic direct-tag/direct-interaction textual flow;
5. only then implement B-016 autonomous scheduling, quotas, budgets, retries,
   idempotency and kill switches.

Do not reimplement provider adapters, social/memory persistence, article generation,
generation traces or semantic reaction selection.

### R4 — Production hardening

- Finish hermetic server-profile/CI verification where external coupling remains.
- Resolve B-012's typed failure-contract decision before replacing ambiguous
  Cyanide-client null/error behavior.
- Document the operational gaps that still require tribal knowledge: image
  load/versioning, secret ownership, persistent volumes, startup/health ordering,
  Cloudflare Tunnel diagnosis, upgrade/rollback and local tar cleanup.

ARM64 builds and the Raspberry Pi compose topology are baseline capabilities, not an
unimplemented deployment milestone.

## Cross-cutting invariants

- `dev` is the integration branch; do not infer current behavior from stale `main`.
- No secret, Steam credential, API key, Auth0 token, MongoDB URI or replay IP address is
  committed or emitted in diagnostics.
- Discovery never enables collection implicitly. The existing discovery flow is
  considered complete; do not add autonomous external polling unless it becomes a
  concrete product requirement.
- Competition behavior is metadata/configuration-driven, not tournament-name hardcoded.
- B-021 through B-024 are completed foundations. If a historical season, bracket,
  discovery candidate or match-detail record is wrong, create a focused regression/data
  defect rather than reopening those broad cards.
- `Phase` is implemented foundation between `Season` and `Stage`. Preserve the
  phase-aware hierarchy; phase-less stages exist only as legacy/compatibility data and
  are not evidence that the Phase model still needs to be designed.
- AI-backed authors are canonical users; provider/model is provenance, not identity.
- AI narrative receives in-universe sporting facts rather than replay/parser/dice/RNG
  implementation terminology.
- New behavior gets regression coverage and completed backlog/roadmap work is marked
  complete in the same change.

## Documentation ownership

| Document | Purpose |
| --- | --- |
| `ROADMAP.md` | Remaining work and execution order only |
| `BACKLOG.md` | Detailed active cards plus compact completed history |
| `CHANGELOG.md` | User/developer-visible work already implemented |
| `backend/docs/AI_ROADMAP.md` | Remaining AI execution order |
| `backend/docs/AI_ARCHITECTURE.md` | Canonical AI design contract/invariants |
| `backend/docs/MATCH_ARTICLES.md` | Match-article authorization/lifecycle/API |
| `backend/docs/AI_REPORT_GENERATION.md` | Current vs target AI report-generation behavior |
| `pybb3-service/README.md` | Cyanidebowl-to-pybb3 boundary and version pinning |

If implementation invalidates a documented contract, update the owning document rather
than adding a competing roadmap.
