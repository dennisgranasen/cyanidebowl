# Development backlog

> Handoff target: Codex  
> Working branch: `dev`  
> Reviewed: 2026-09-07  
> Repository: `dennisgranasen/cyanidebowl`

Work only on `dev` or a short-lived branch created from `dev`. Do not base work on
`main`.

## Goal

Stabilize the current `dev` implementation before expanding the new
league-system/season/stage model. Fix confirmed functional and security problems first,
then make the stage work reachable, testable, and documented.

## Rules for Codex

1. Read every affected file completely before editing it.
2. Preserve the new `LeagueSystem`, `Season`, `Stage`, `StageSource`,
   `MatchInterpretation`, archive-provider, and match-adapter work already on `dev`.
3. Never commit `.env`, database credentials, Auth0 tokens, Cyanide API keys, Discord
   tokens, database dumps, or generated data.
4. Keep Cyanide-disabled local development as the safe default.
5. Make one focused commit per backlog item and add tests for behavior changes.
6. If product behavior is unclear, document the question and stop that item instead of
   inventing behavior.


7. Preserve the canonical authorization model: Auth0 subject identifies the user,
   `coachClaims` are game-aware coach identities, `siteAdmin` is the super-role, and
   LeagueSystem administration may be global or scoped to specific LeagueSystems.## Priority overview

| Priority | Item | Outcome | Status |
| --- | --- | --- | --- |
| P0 | B-001 | Contest fetching works again | Done |
| P0 | B-002 | Public header-reflection endpoint is removed | Done |
| P0 | B-003 | Mutation routes reach method security correctly | Done |
| P0 | B-004 | Authentication/authorization has regression tests | Done |
| P1 | B-005 | Frontend permission state is deterministic | Partial: targeted hook tests pending |
| P1 | B-006 | Stage aggregation handles empty/scoped data correctly | Done |
| P1 | B-007 | The new stage model has an application/API entry point | Done: read API and LeagueSystem admin CRUD |
| P1 | B-008 | Frontend API duplication and malformed URLs are removed | Done |
| P2 | B-009 | Profiles, auth configuration, and docs agree | Done |
| P2 | B-010 | GitHub CI validates backend/frontend and builds ARM64 artifacts | Implemented; test isolation pending green verification |
| P2 | B-011 | Stale repository metadata/docs are cleaned up | Done |
| P3 | B-012 | Broad exception/null handling is improved incrementally | Blocked: failure contract decision needed |
| P1 | B-013 | Admins can search Cyanide and prepare LeagueSystem sources | Done |

## Current status


- User authorization now uses canonical, game-aware `coachClaims`. Legacy parallel
  coach/admin representations must not be reintroduced. `siteAdmin` acts as the
  super-role; LeagueSystem administration supports both global and scoped grants.
- GitHub Actions now runs backend and frontend tests for `dev`/PRs and builds the
  three ARM64 application images as an Actions artifact only after both suites pass.
  The remaining B-010 work is to finish isolating server-profile tests from MongoDB,
  schedulers and external Cyanide calls and verify the workflow green.- B-005 implementation is verified by the frontend build and existing suite; add
  dedicated hook tests for loading, token failure, backend failure, and logout.
- The legacy circuit-admin routes are retained for compatibility but `/admin` now
  manages the LeagueSystem hierarchy.
- The home page now groups content by LeagueSystem with seasons, stages, and
  recent results. News, standings, playoff trees, and statistics remain follow-up work.
- B-012 needs a product decision before changing `CyanideRestApiClient`:
  callers currently treat `null` as both upstream unavailability and an internal
  client failure. Decide whether scheduled collection should skip, retry, or fail
  the job for each outcome before replacing that contract.

---

## P0 â€” Must fix before feature work

### B-001 â€” Restore contest-fetch candidate collection

**Confirmed problem**

In `backend/src/main/java/net/warp_scores/warpscores/service/FetchDataService.java`,
`fetchCompetitionContests()` creates `competitionsToCollect`, but its stream has no
terminal operation and never adds anything to the list. Consequently,
`loadContestsForCompetitions(...)` always receives an empty list.

```java
List<Competition> competitionsToCollect = new ArrayList<>();
competitionsNeedingContests.stream()
        .filter(c -> this.shouldLoadContests(c, lastMatchDateByCompetitionId));
```

**Implementation**

- Replace the dead stream with a collected list or equivalent clear implementation.
- Preserve the existing `shouldLoadContests` rules.
- Test in-progress, live-contest, recently played, stale inactive, and
  scheduler-disabled cases.

**Acceptance criteria**

- Eligible competitions reach `loadContestsForCompetitions`.
- Ineligible competitions do not trigger Cyanide API calls.
- Tests fail against the old implementation and pass after the fix.

### B-002 â€” Remove the public `/debug-headers` endpoint

**Confirmed problem**

`DebugController` reflects every incoming header, and `SecurityConfiguration` exposes
`GET /debug-headers` publicly. This can expose `Authorization`, cookies, proxy headers,
and other sensitive values.

**Files**

- `backend/src/main/java/net/warp_scores/warpscores/controller/DebugController.java`
- `backend/src/main/java/net/warp_scores/warpscores/config/SecurityConfiguration.java`

**Implementation**

- Delete the controller and public matcher if no longer needed.
- If diagnostics are required, make them `dev`-only and return a fixed allowlist of
  non-sensitive fields. Never return arbitrary headers.

**Acceptance criteria**

- Server profile returns 401, 403, or 404 for `/debug-headers`.
- No endpoint reflects arbitrary headers.
- Production behavior has a regression test.

### B-003 â€” Align HTTP security with mutation controllers

**Confirmed problem**

Controllers use `@PreAuthorize` for circuit and contest mutations, but their HTTP
matchers are commented out in `SecurityConfiguration`. Since the chain ends with
`.anyRequest().denyAll()`, valid requests can be rejected before method security runs.

Known affected families include:

- `POST/DELETE /circuits/**`
- `POST /contests/**`

Inspect every non-GET controller mapping; do not rely only on this list.

**Implementation**

- Create an endpoint/method matrix for all controller mappings.
- Keep read-only public routes public.
- Let protected mutations through the filter chain only when authenticated.
- Keep specific permission checks at method level using `@PreAuthorize`.
- Confirm and document stateless Bearer-token and CSRF behavior.
- Keep `.anyRequest().denyAll()` as the fallback.

**Acceptance criteria**

- Anonymous mutation: 401.
- Valid token without permission: 403.
- Valid token with permission: reaches the controller.
- Unknown route remains denied.
- Every mutation endpoint appears in an automated security test.

### B-004 â€” Add authentication/authorization regression tests

Cover:

- public GET without token;
- protected endpoint without token;
- malformed/expired token;
- wrong issuer and wrong audience;
- correct issuer/audience;
- `permissions` mapping without `SCOPE_` prefix;
- missing permission;
- anonymous and authenticated `/userPermissions`;
- intended RS256 restriction.

Use mocked JWT/security tests. Do not call Auth0 or require credentials/network access.
Test the explicit `JwtDecoder`, not only YAML properties.

---

## P1 â€” Stabilize current development work

### B-013 â€” Search Cyanide from LeagueSystem admin

**Implementation**

- Search Cyanide leagues by name or ID and Blood Bowl version from `/admin`.
- Show the league and competition metadata returned by the lookup flow without
  fetching matches, contests, teams, or ranks.
- Let an administrator prepare a returned competition as a season/stage source for
  the selected LeagueSystem; saving remains an explicit separate action.
- Register a saved `StageSource` for normal data collection without resetting an
  existing collection checkpoint.

**Acceptance criteria**

- Searching alone never enables collection and never requests matches.
- A search result can prefill the existing season/stage/source workflow.
- Collection starts only when the administrator saves the `StageSource`.
- Frontend tests and production build pass.

### B-005 â€” Simplify and fix frontend permission loading

**Confirmed problems** in
`frontend/src/hooks/useAuth0WithUserPermissions.jsx`:

- `authenticationReady` depends on `isLoading` and `permissionsLoading`, but its effect
  only listens to `userPermissions`;
- after `.catch(...)`, the chain continues to `.then(setUserPermissions)` and can set
  permissions to `undefined`;
- four state variables/effects coordinate one fetch and have incomplete dependencies.

**Implementation**

- Refactor to one clear effect driven by Auth0 loading/authentication state.
- Always retain a complete deny-by-default permission object.
- Anonymous users must not request a token.
- On failure, remain deny-by-default and expose deterministic ready/error state.
- Avoid state updates after unmount.
- Test anonymous, authenticated, loading, token failure, backend failure, and logout.

**Acceptance criteria**

- `userPermissions` is never `undefined`.
- `authenticationReady` follows the loading flags.
- Failure never reveals protected UI or creates repeated popup/token loops.

### B-006 â€” Harden stage match aggregation

**Files**

- `backend/src/main/java/net/warp_scores/warpscores/service/StageMatchService.java`
- `backend/src/test/java/net/warp_scores/warpscores/service/StageMatchServiceTest.java`
- related repositories/archive providers

**Confirmed/likely issues**

- Empty sources can calculate `lastIndex = -1` and fail validation instead of returning
  an empty result.
- Every request calls `MatchInterpretationRepository.findAll()` and scans the full list
  for every match.
- First-source-wins duplicate behavior is implicit and undocumented.

**Implementation**

- Return an empty result for an empty valid source.
- Query only interpretations relevant to the requested stage/matches.
- Document and test duplicate precedence.
- Add tests for missing stage, no sources, no matches, missing/reversed boundaries,
  consolidated/archive duplicates, cross-source duplicates, BB1/BB2/BB3 adapters, and
  excluded/replacement interpretations.

**Acceptance criteria**

- Empty valid data returns an empty list.
- Work is bounded to the requested stage and its matches.
- Boundary and duplicate behavior is deterministic and tested.

### B-007 â€” Connect the stage model to an application boundary

`dev` contains models, repositories, adapters, archive providers, and
`StageMatchService`, but no clearly discoverable controller or documented command for
consuming stage matches.

Before implementation, document decisions for:

- read-only endpoint or internal consumer;
- canonical IDs and URL encoding;
- response DTO;
- error mapping;
- whether writes are migration-only or an admin API.

Do not invent write APIs before these decisions are made.

**Suggested first slice**

- Add a read-only endpoint for one stage's matches.
- Return 404 for missing stage and 400 for invalid configuration/boundaries.
- Prefer an API DTO over exposing persistence models.
- Add controller/service integration tests.
- Document minimal seed/migration documents for league system, season, stage, and
  source.

### B-008 â€” Clean up `WarpScoresApiService.jsx`

**Confirmed problems**

- `competitionMatches` is declared twice; the latter overwrites the former.
- `competitionTeams` generates a URL with a trailing space.
- A large obsolete Java example remains commented inside JSX.
- Authenticated helper signatures are inconsistent.

Remove the duplicate, whitespace, and dead code; normalize helper signatures; and add
request tests asserting method, URL, body, and authentication header. Do not alter API
semantics during this cleanup.

---

## P2 â€” Build and documentation hygiene

### B-009 â€” Make profile and auth configuration consistent

**Confirmed mismatch**

- Fly uses `SPRING_PROFILES_ACTIVE=server`.
- production security uses `@Profile("server")`.
- README tells production users to set `SPRING_PROFILES_ACTIVE="production"`.

Preserve `server` unless there is a reason to rename it. Ensure production security
cannot disappear due to a profile typo. Clarify whether the explicit `JwtDecoder` or
YAML owns issuer/JWKS/algorithm configuration. Fail server startup clearly for absent or
unsafe required configuration. Keep the Auth0 audience identical in frontend/backend,
and document which values are public identifiers versus secrets.

### B-010 â€” Add GitHub CI for `dev` and pull requests

The repository now contains a GitHub Actions workflow for pushes and pull requests
targeting `dev`.

**Implemented**

- Backend and frontend tests run before image creation.
- Pull requests run tests only.
- Successful pushes to `dev` build the ARM64 backend, frontend and pybb3 images.
- The three images are exported as `cyanidebowl-arm64.tar.gz` with a SHA-256 checksum
  and retained as a GitHub Actions artifact; no container registry is required.
- Frontend build-time configuration comes from GitHub repository variables.
- Production runtime secrets remain on the deployment host and are not copied into
  GitHub merely to build images.

**Remaining verification**

- Server-profile tests must be hermetic: no live MongoDB dependency, scheduler work,
  Cyanide API calls, Auth0 calls or pybb3 network calls.
- Controller integration tests must mock all persistence/service collaborators they
  exercise, not only the primary service under test.
- The workflow is only considered complete when both test jobs and the ARM64 artifact
  job pass from a clean GitHub runner.

Do not solve CI failures by adding production MongoDB, Cyanide, Auth0 or other runtime
credentials to GitHub.
### B-011 â€” Refresh repository metadata and docs

- Update root `pom.xml` SCM URLs if GitHub is authoritative.
- Replace stale GitLab badges/issues or label them as upstream references.
- Fix â€œAuth0 Prodiverâ€.
- Document modules, safe VS Code launch configurations, Fly secret names without
  values, and archive settings/default-disabled behavior.
- Keep `.env.example` value-free.

---

## P3 â€” Incremental maintainability

### B-012 â€” Replace broad exception/null fallbacks with explicit outcomes

Do this one service/controller at a time, with a regression test before each behavior
change. Distinguish not-found, invalid input, upstream unavailable, rate limiting, and
internal failure. Centralize HTTP error mapping where practical. Never expose stack
traces, secrets, sensitive URLs, or full upstream payloads. Remove production
`System.out.println` calls.

Initial candidates:

- `CyanideRestApiClient`
- `LookupController`
- `CompetitionController`
- `ContestController`
- `CircuitController`
- `CyanideApiService`

## Already fixed on `dev` â€” do not reintroduce

- Cyanide request logging sanitizes the URI instead of logging the API key.
- Full upstream response bodies are no longer logged.
- Deployment secrets are excluded from Git; `.env.example` contains names only.
- Cyanide-disabled local launch configurations remain the safe default.
- Stage/season/league-system models and adapters are active work, not dead code.

- Canonical user permissions are resolved through `UserPermissionService`; do not
  restore parallel legacy admin lists.
- Coach ownership is represented by game-aware `coachClaims`, not a bare global list
  of coach IDs.
Add a URI-sanitization regression test if absent, but do not restore the older `main`
implementation.

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

- `git status --short` contains no `.env`, dumps, tokens, or generated artifacts.
- Changed logs contain no API keys, Authorization headers, cookies, MongoDB URIs, or
  full upstream response bodies.
- Server tests require no live network access.

## Definition of done per item

- Automated tests cover the behavior.
- Relevant commands pass.
- Configuration/public behavior changes are documented.
- No secrets or generated artifacts are committed.
- The commit is focused and references the backlog ID.
- Status is updated only after verification.

---

## Editorial & Community

### B-014 — Editorial/community layer

**Implemented by the editorial/community patch**

- Site-wide and LeagueSystem-scoped editor grants, separate from LeagueSystem administration.
- Draft/published/archived articles with optional cover image, season, tags, channels,
  featured flag, slug and legacy-source metadata.
- TipTap WYSIWYG article editor and public article/news views.
- Generic comments for articles and matches. Match comments derive HOME_COACH /
  AWAY_COACH from canonical game-aware coach claims; all other authenticated users
  comment as spectators.
- Soft-delete moderation for comment owners, scoped editors and site administrators.
- POW / double POW / triple POW and skull / double-skull / triple-skull reactions.
  One active reaction per authenticated user and target.
- 0–10 per-user player ratings for players who actually participated in the match,
  with match/season/career aggregation and coach/spectator breakdown.
- Frozen match-player participation records. A player carrying suspendedNextMatch from
  the team's preceding match is represented as MNG and rejected by the rating endpoint.
- Bulk legacy article import endpoint keyed by legacySource, suitable for migrating the
  historical Google Sites material after extraction.

### B-015 — Enable team comment streams

**Status: Blocked**

The generic community target model already contains TEAM, but TEAM comments are
deliberately rejected by the API until the team endpoint/canonical team identity is
stable. Do not implement a second team-comment model.

**Unblock when**

- one canonical historical/current team ID is defined for supported games;
- the team endpoint reliably resolves that ID;
- LeagueSystem ownership/scope can be resolved for a team.

**Then**

- enable TEAM in `EditorialCommunityService.comments/addComment`;
- render `CommentThread targetType="TEAM"` on the team page;
- apply the same scoped-editor/site-admin moderation rules.

---

## AI editorial operations

### B-016 — AI reporter editorial scheduling and cost control

**Status: Backlog — intentionally deferred**

Implement after the canonical reporter profiles and first end-to-end AI editorial flow
are stable.

- editorial scheduling and queue/backpressure
- per-reporter/global generation quotas
- provider/token/cost budgets
- cooldowns, retries and idempotency
- admin visibility and kill switches
- priority handling for direct tags

**Product rule:** a directly tagged reporter is scheduled for a textual response when
enabled and capable. Tagged work bypasses probabilistic selection; hard admin/global
disables still win.


## AI foundation prerequisites for B-016

The following foundation items intentionally come after B-016 numerically because
B-014–B-016 already have established meanings. Execution order is defined by
`AI_ROADMAP.md`: complete B-017 through B-020 before implementing B-016.

### B-017 — Canonical User and AI identity foundation

**Status: Implemented foundation — prerequisite for B-016**

- Use one canonical `User` with `accountType = HUMAN | AI`.
- Treat older `USER | AI_AGENT` terminology as obsolete.
- Keep roles/capabilities separate from coach claims, team affinity and community
  relationships.
- Keep generation provenance separate from user identity.
- Preserve existing Auth0 subject, permission and game-aware `coachClaims` behavior.

**Acceptance criteria**

- Human and AI authors use the same editorial/community primitives.
- Coach remains a contextual relationship, not an account type.
- Generated content can identify both canonical author and provenance.
- Tests cover the separation between identity, capability, relationship and provenance.

### B-018 — Canonical AI context and world-model contract

**Status: In progress — prerequisite for B-016**

**Implemented slices**

- AI-003: canonical `SubjectRef` / `ContextItem` retrieval with explicit source and authority separation.
- AI-004: deterministic `ContextProfile`, `ContextPlanner`, `ContextAssembler`, bounded selection and shared world-model policy.
- AI-005: canonical provider request/response contract, provider registry and configurable provider/model routing.
- AI-006: Gemini `LlmProvider` adapter using the current Interactions API with structured output, usage metadata and normalized provider failures.
- AI-007: reusable OpenAI Responses-compatible provider instances plus ordered retryable fallback; xAI/Grok is the first configured endpoint.
- Reporting migration to `CanonicalLlmRequest` is in progress.

- Implement the canonical context envelope: `thread`, `social`, `self`, `discourse`,
  `memory`, `domain`.
- Resolve contextual relationships at generation time.
- Include human and AI statements in the same discourse model.
- Enforce the shared in-universe rule that Blood Bowl is a real sport.
- Translate internal mechanics before they reach AI-facing narrative context.

**Acceptance criteria**

- Context assembly is deterministic/testable without a provider call.
- The same context contract supports comments, replies, tags and articles.
- Tests prevent dice/RNG/game/replay/simulation meta-language from leaking into the
  AI-facing world model.

**Remaining**

- persisted/retrievable `social` and `memory` context;
- broader semantic projection of mechanical source data into in-world domain facts;
- integration of canonical context into the first persisted generation flow.

### B-019 — Dedicated Fans community population reconciliation

**Status: Backlog — prerequisite for B-016**

- Desired active AI-backed `COMMUNITY_MEMBER` population equals team Dedicated Fans
  value (currently 1–6).
- First discovery creates clear team-affine personas.
- Population increases reactivate inactive matching users before creating new users.
- Population decreases deactivate surplus users without deletion.
- Preserve authored history, reactions, memory, relationships and provenance.

**Acceptance criteria**

- Reconciler supports create/reactivate/deactivate/unchanged.
- Unchanged input is idempotent.
- Reactivation preserves canonical user identity.
- No provider, scheduler or network access is required by reconciliation tests.

### B-020 — First end-to-end AI editorial flow

**Status: Backlog — prerequisite for B-016**

Build one narrow deterministic flow before autonomous scheduling:

- enabled AI user;
- supported target/thread;
- explicit direct tag or equivalent deterministic trigger;
- canonical context assembly;
- provider abstraction;
- persisted generation provenance;
- persisted response through existing editorial/community primitives.

**Acceptance criteria**

- The flow uses B-017/B-018 contracts rather than feature-local AI models.
- Shared world-model policy is applied centrally.
- Retries do not create duplicate persisted responses.
- B-016 remains deferred until this flow is stable.
