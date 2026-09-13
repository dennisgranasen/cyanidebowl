# Changelog for [warp-scores](README.md) 

## Unreleased

- Added OpenAI-rendered community images with local asset serving, a full Site Admin
  Community editor, and team-page supporter cards linked to public fan profiles.

- Added a public Community directory/profile view, durable fan avatar/profile-media requests,
  and configurable periodic Dedicated Fans reconciliation for purchases between matches.

- Added fan visual profile briefs and avatar/profile-photo metadata, including broad
  supporter archetypes and team-colour hints for future image generation.

- Added generated public Dedicated Fan profiles with behavioural personality dimensions,
  Site Admin editing and configurable loyalty switches when opponent fan counts move in
  opposite directions after a match.

- Added queued Staff comments on general articles: publication creates stable per-reporter
  candidates, policy/probability is evaluated once, and approved comments use existing
  reporter quota, global admission and canonical provenance.

- Added the first finalized-match Dedicated Fan queue producer: completed matches can
  create one stable supporter-comment candidate per league system, evaluated once through
  the existing initiative policy and executed through canonical AI context/provenance.

- Added a durable autonomous AI work queue with cross-process dedupe, atomic leases,
  explicit priority, retry/backoff, terminal failure visibility and site-admin queue/budget
  observability. Candidate discovery remains policy-driven and domain-specific.

- Added team-affine AI fan interactions: general articles support explicit canonical
  team links, and active fan identities can comment/reply on eligible articles and match
  threads, including deterministic own-coach affinity without turning fans into reporters.

- Added AI initiative policy with site defaults and LeagueSystem-scoped editor overrides;
  Staff autonomy and team-affine fan commenting are modeled separately with safe defaults.

- Added per-reporter autonomous activity quotas/cooldowns backed by existing runtime state;
  spontaneous comments/replies/reactions are gated after idempotency checks, while direct
  tags bypass autonomous quotas but still retain global hard generation budgets.

- Added central AI generation admission controls with an opt-in global kill switch,
  concurrency backpressure and daily generation/token budgets backed by durable generation
  traces; admin endpoints expose limits and current usage. Autonomous scheduling remains separate.

- Added deterministic direct AI replies for explicit `@alias` / `@reporter-id` tags;
  direct tags bypass reply probability while preserving capability checks, canonical context,
  ordinary community-comment persistence, provenance and retry idempotency.

- Added Dedicated Fans community reconciliation: each team's latest known Dedicated Fans
  value controls a persistent AI-backed COMMUNITY_MEMBER population, reactivating identities
  before creating new ones and deactivating surplus members without deleting history.

- AI article generation now preserves replay chronology in context while explicitly
  forbidding technical turn/round counters in published prose; reporters must translate
  those values into natural in-world timing when chronology matters.

- Completed the R2 Staff profile work: HUMAN public profile validation/fallback policy,
  canonical frontend profile/deep-link helpers and regression tests.
- Completed AI Staff public-profile normalization with deterministic display/role/image
  fallbacks and a short summary derived from existing public reporter copy.

### Changed

- Moved LeagueSystem selection into the primary application navigation and made the
  selected system URL-addressable on the LeagueSystem landing page.
- Removed the superseded Circuit/CircuitLeg frontend and backend domain, routes and APIs;
  LeagueSystem/Season/Phase/Stage/StageSource is now the sole competition hierarchy.
- Completed localization of the immediate match roster/replay modal surface and added
  regression checks against hard-coded labels and identifier-corrupting replacements.
- Added a shared public `Staff` surface for HUMAN editors and AI reporters; `Redaktion`
  remains only the Swedish UI translation.
- Added self-editable public HUMAN Staff profile fields that are separate from
  authentication identity and initially seeded from available OAuth profile data.
- Hardened Staff profile lifecycle behavior: the synthetic dev account does not persist
  a profile, permission loss removes public visibility without deleting identity/history,
  and OAuth re-login does not overwrite initialized local public-profile edits.
- Added explicit public AI reporter profile type/display-name fields while keeping
  provider/prompt/runtime configuration out of the public profile contract.
- Added Staff-profile parent navigation and localized the R2 match-card/immediate
  match-modal surface through react-intl.

- Refreshed project/AI roadmaps and documentation ownership so completed foundation work,
  current priorities and external pybb3 dependencies are clearly separated.
- Added admin-assisted LeagueSystem season/source discovery and optional email
  notifications for new candidates.
- Added the public, read-only stage-match API and stage seed documentation.
- Replaced the legacy circuit-admin landing page with LeagueSystem hierarchy CRUD.
- Made the home page LeagueSystem-first, with season/stage structure and recent results.
- Defaulted LeagueSystem result cards to their latest played season with a season menu.
- Hardened server HTTP/JWT security and stage-match aggregation.
- Simplified frontend permission loading and normalized API request URLs.
- Updated GitHub metadata and the `server` production-profile documentation.
- Reworked user authorization around canonical, game-aware `coachClaims`, with
  `siteAdmin` as the super-role and global/scoped LeagueSystem administration.
- Added site-admin user management and made My Teams ownership game-aware.
- Added local race-logo fallbacks for Old World Alliance, Khemri/Tomb Kings and
  Chaos Renegades where Cyanide assets are unavailable or ambiguous.
- Added GitHub Actions validation for backend/frontend changes and ARM64 Docker image
  artifacts for Raspberry Pi deployment after successful tests.
- Kept production runtime secrets out of GitHub Actions; frontend public build-time
  values are repository variables while deployment secrets remain host-local.
- Isolated server-profile tests from scheduled/background work and persistence where
  those dependencies are outside the behavior under test.

### Description

Initial release after various SNAPSHOT releases in early phase of project

### Features

- Scheduled pulls from Cyanide API
- UI representation for leagues, competitions, teams, ranks, contests and match details
- Support for Wissen (Swiss), RoundRobin and Knockout competition formats
- Check for API Status by Cyanide and pause scheduled pulls on status 'down'
- Show latest and live matches of a league
- OAuth (using Auth0) with existing discord accounts
