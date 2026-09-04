# Replay analysis patch

Patch base: GitHub `dev` commit `3a300446c587090dde24c103aff038944f59a916`.

This revision also declares the nested MongoDB queries explicitly. `participantTotals` intentionally contains flexible
maps, so Spring Data cannot derive those property paths from Java method names alone.

Unpack this archive in the cyanidebowl repository root and overwrite the included files.

## Rebuild

```bash
docker compose --profile pybb3 up -d --build pybb3-service
mvn clean install -Pserver -DskipTests
```

Restart the backend after rebuilding. New replay downloads create both `.xml.gz` and compact `.json.gz` artifacts.
Existing downloaded replays are analyzed newest-first, one per configured interval.

Configuration defaults:

```yaml
replay-analysis:
  enabled: true
  initial-delay-ms: 120000
  fixed-delay-ms: 600000
```

The compact artifact contains all ordered replay events and a full BoardState checkpoint whenever phase, active team,
or either team's `GameTurn` changes. Searchable extracted facts and aggregate counters are stored in MongoDB; replay
blobs remain in `REPLAY_SWEEPER_STORAGE_DIRECTORY` / the `replay-data` Docker volume.

Admin endpoints:

- `GET /admin/replay-sweeper/replays`
- `POST /admin/replay-sweeper/replays/{matchId}/analyze`

Public read endpoints:

- `GET /matches/{matchId}/replay`
- `GET /matches/{matchId}/replay-analysis`
- `GET /matches/{matchId}/replay/original`
- `GET /replay-statistics?coachId=...&teamId=...`

The match modal now contains a Replay tab with analysis and original replay download. LeagueSystem match cards show a
replay marker. Recent matches are checked immediately for Cyanide player details but are only classified as missing
after the configured settling period. Replay fetching and the admin work list use the configurable
`REPLAY_SWEEPER_AVAILABILITY_WINDOW_DAYS` (30 by default).

Verification performed here: Python syntax compilation, focused parser test, and `git diff --check`. Maven and frontend
dependency tools were unavailable in the packaging environment, so run the project build above locally.
