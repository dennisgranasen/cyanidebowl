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

- `LeagueSystem -> Season -> Stage -> StageSource` domain model, read APIs and admin CRUD;
- LeagueSystem-first public navigation with season/stage selection;
- interactive Cyanide league/competition discovery and explicit source registration;
- existing round/group/playoff presentation infrastructure, including replay/rematch
  handling;
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

### R1 — Competition correctness and presentation polish

Work from known defects in the existing LeagueSystem UI rather than rebuilding the
competition model.

- Make round grouping deterministic when source scheduling/order produces the wrong
  round count.
- Regression-test known multi-group seasons where groups are on different current
  rounds.
- Close remaining standings/bracket presentation defects using the existing metadata
  and bracket model.
- Preserve every replay/rematch in a series and keep individual on-field results
  inspectable.
- Keep pre-penalty/on-field scores separate from administrative/adjudicated outcomes
  where both are available.
- Add a separate `Phase` layer only if a concrete competition structure cannot be
  represented cleanly with the existing Season/Stage model.

Exit condition: known historical seasons render rounds, groups and playoff series
correctly without tournament-name special cases.

### R2 — Automated discovery

Interactive discovery and explicit `StageSource` registration already exist. Remaining
work is proactive discovery:

- normalize provider-specific candidate/watch data at the integration boundary;
- periodically suggest likely new competitions from registered leagues/coaches;
- allow incomplete candidates to mature idempotently;
- deduplicate repeated suggestions;
- keep discovery lightweight;
- never enable collection until an administrator explicitly saves a source.

Exit condition: likely new season/stage sources are surfaced automatically without
changing collection state.

### R3 — Historical match and replay quality

- Close known historical roster/race/team-kind enrichment gaps through the canonical
  match-detail pipeline.
- Keep expensive enrichment/backfill out of overview rendering.
- Consume a versioned pybb3 timeline contract through `pybb3-service`.
- Render a roll only when an actual test occurred and expose the semantic reason for
  that test.
- Consume upstream `skill_rerolls` separately from team/generic rerolls while retaining
  compatibility with older payloads.
- Preserve explicit unknown-event provenance and replay IP redaction.

Detailed replay parsing belongs in the separate `pybb3` repository. Cyanidebowl owns
the service contract, validation, compatibility and presentation.

Exit condition: current and historical match views use stable normalized contracts and
do not invent or mislabel replay events.

### R4 — Remaining AI product work

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

### R5 — Production hardening

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
- Discovery never enables collection implicitly.
- Competition behavior is metadata/configuration-driven, not tournament-name hardcoded.
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
