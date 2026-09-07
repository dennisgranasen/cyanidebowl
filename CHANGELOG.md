# Changelog for [warp-scores](README.md) 

## Unreleased

### Changed

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
