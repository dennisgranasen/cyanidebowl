# BlaskScore features

This is a short, evolving overview of what BlaskScore adds to the original
warp-scores/cyanidebowl foundation. It is not a release history; see
`CHANGELOG.md` for implemented changes and `ROADMAP.md` for remaining work.

## Inherited foundation

- League, competition, team, standings, match-result and match-detail views.
- Cyanide discovery and scheduled result imports.
- Swiss, round-robin and knockout competitions.
- Auth0 login for Discord accounts.

## BlaskScore additions

### Competition and replay

- A canonical `LeagueSystem -> Season -> Phase -> Stage -> StageSource` model.
- LeagueSystem-first navigation, standings, group rounds and playoff brackets.
- Deterministic BB3 replay download, analysis and reanalysis, separate from AI work.
- An authenticated, versioned `pybb3-service` boundary.

### Editorial and community

- General and match articles with associations to competitions, teams and players.
- Public Staff profiles for human editors and AI reporters.
- Review, provenance and configurable acceptance policies for AI-authored content.
- Community directory, public fan profiles and team-affine community interactions.
- Durable community-media and Dedicated Fan reconciliation jobs.

### AI operations

- Canonical human/AI identities, reporter profiles and persisted context.
- Autonomous, deduplicated AI work with retry, cooldown and administrator diagnostics.
- Provider-aware text queues, generation budgets and an explicit global stop control.
- Renderer-neutral image requests that never fall back to paid generation by default.

### Deployment and localization

- ARM64/Raspberry Pi Compose deployment behind Cloudflare Tunnel.
- Persistent replay, community-media and pybb3 credential volumes.
- Swedish, English, Spanish, Finnish and Polish user-interface localization.

## Current limitations

- Replay visualization is not yet interactive; deterministic state reconstruction is the
  next prerequisite.
- BB1 and BB2 adapters depend on upstream support.
- Image generation waits for free provider quota; paid fallback is not configured.

## Keeping this current

Update this file only when the product capability changes. Keep implementation detail,
configuration values and release-by-release history in their owning sources.