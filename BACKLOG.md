# Development backlog

> Handoff target: Codex  
> Working branch: `dev`  
> Reviewed: 2026-09-01  
> Repository: `dennisgranasen/cyanidebowl`

Work only on `dev` or a short-lived branch created from `dev`. Do not base work on
`main`. At review time, `dev` is 15 commits ahead of and one merge commit behind
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

## Priority overview

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
| P2 | B-010 | GitHub CI validates backend and frontend | Deferred |
| P2 | B-011 | Stale repository metadata/docs are cleaned up | Done |
| P3 | B-012 | Broad exception/null handling is improved incrementally | Blocked: failure contract decision needed |

## Current status

- B-005 implementation is verified by the frontend build and existing suite; add
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

No active GitHub build workflow was found. Add a least-privilege, non-deployment
workflow that:

- runs for PRs and pushes to `dev`;
- uses the required Java version;
- tests/packages backend modules without Docker or external secrets;
- runs `npm ci`, frontend tests, and production build;
- caches Maven/npm dependencies;
- never contacts Cyanide, Atlas, Auth0, Discord, or Fly.

If existing tests require services, isolate them or label true integration tests; never
add fake production secrets to CI.

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

Add a URI-sanitization regression test if absent, but do not restore the older `main`
implementation.

## Verification

### Backend

```bash
mvn clean test -P server -DskipDocker -pl api,cyanide-api,backend -am
mvn clean package -P server -DskipDocker -pl api,cyanide-api,backend -am
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