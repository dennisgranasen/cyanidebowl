# BB3 replay sweeper

The replay sweeper downloads missing BB3 replay XML in newest-first batches. Defaults are `0 0 5 * * *` at
`Europe/Stockholm`, i.e. 05:00 local Swedish time. Administrators can change enabled state, cron expression, time zone,
batch size and Steam username in the admin page, run a batch manually, renew Steam Guard authentication and inspect
the latest 50 log entries.

The **Sync replays** button starts the same background sweep immediately. A run claims the single execution slot
before it is dispatched to the background worker. Scheduled and manual requests are therefore rejected while a sweep
is active instead of being queued. The button is disabled until that run finishes.

## Secrets and persistence

Set a long stable `PYBB3_CREDENTIAL_ENCRYPTION_KEY` on pybb3. The Steam refresh ticket is Fernet-encrypted in the
`pybb3-credentials` volume and never returned to the browser or stored in MongoDB. Replacing the key makes the old
credential unreadable and intentionally requires a new admin login. Password and Guard code are never stored.

The original double-base64/zlib `ReplayData` payload is stored byte-for-byte as a client-compatible `.bbr` in
`REPLAY_SWEEPER_STORAGE_DIRECTORY`; production compose mounts the `replay-data` Docker volume there. A compact
`.json.gz` derivative is stored beside it. The compact stream retains
every ordered event and a complete semantic board-state checkpoint whenever the phase, active team or either team's
`GameTurn` changes. In other words, it stores a board state for every turn without duplicating it after every event.

MongoDB stores the schedule, run state, replay file index, a 90-day administrative event log and searchable replay
analysis. Replay blobs themselves are deliberately not stored in MongoDB. The analysis contains dice groups and
faces, roll metadata and outcomes, reroll/apothecary/wizard events, regeneration/resurrection and other special event
payloads, plus per-team/per-coach aggregate counters. Unknown Cyanide enum values remain intact rather than being
guessed, so a newer parser can reinterpret them later.

Existing replay files are analyzed newest-first in the background. A replay is checked once per parser version;
failed or data-less replays are not retried until the parser version changes or an administrator clicks **Analyze
again**. Public consumers can read one match at `GET /matches/{matchId}/replay-analysis` or aggregate counters through
`GET /replay-statistics?coachId=...&teamId=...`.

The match statistics modal has a Replay tab with artifact status, extracted dice/resource/special-event summaries and
download of the untouched original replay through `GET /matches/{matchId}/replay/original`. LeagueSystem match cards
are marked when a replay is present. Storage paths are never exposed to the browser.

`replay-sweeper.availability-window-days` defaults to 30 days and is intentionally configurable because Steam's exact
retention is not guaranteed. The sweeper does not waste requests outside that window, and the admin list only shows
recent attempts. A failed recent download is labelled **AT RISK**; a saved replay remains available locally regardless
of Steam retention.

Detailed player data is requested from Cyanide immediately for newly finished matches. If Cyanide has not completed
the detailed record yet, it remains unchecked and is retried; it is only classified as unavailable after the existing
`match-details.backfill.minimum-match-age-hours` settling period.

Administrators can batch-import up to 100 local `.bbr` files. Each replay is decoded and matched to MongoDB using the
embedded `CompetitionInfos.MatchId`; the `.bbr` filename UUID is a fallback. Team names and dates are deliberately not
used as identity. Imported bytes are retained unchanged.

**Inspect** exposes the complete parsed compact JSON with copy and download controls. Parser v1 labels its statistical
interpretation `RAW_UNMAPPED`: event order and semantic board-state checkpoints are retained, but numeric Cyanide enums
must be mapped from inspected real replays before the UI may describe them as verified dice or resource statistics.

## Failure behaviour

- `STEAM_ACCOUNT_ACTIVE`: skip the whole batch, retain the credential, log a warning, retry at the next scheduled run.
- invalid or expired ticket: mark authentication invalid and show an admin warning until it is renewed.
- temporary Steam/BB3/network failure: retain the credential and log the failure.
- a replay unavailable for one game: record that game as failed while retaining successful downloads from the batch.

The BB3 client exists only for the duration of a batch and is closed afterwards, so the service account is not held
online during the day.
