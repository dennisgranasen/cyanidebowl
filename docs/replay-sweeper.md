# BB3 replay sweeper

The replay sweeper downloads missing BB3 replay XML in newest-first batches. Defaults are `0 0 5 * * *` at
`Europe/Stockholm`, i.e. 05:00 local Swedish time. Administrators can change enabled state, cron expression, time zone,
batch size and Steam username in the admin page, run a batch manually, renew Steam Guard authentication and inspect
the latest 50 log entries.

## Secrets and persistence

Set a long stable `PYBB3_CREDENTIAL_ENCRYPTION_KEY` on pybb3. The Steam refresh ticket is Fernet-encrypted in the
`pybb3-credentials` volume and never returned to the browser or stored in MongoDB. Replacing the key makes the old
credential unreadable and intentionally requires a new admin login. Password and Guard code are never stored.

Replay XML is written atomically to `REPLAY_SWEEPER_STORAGE_DIRECTORY`; production compose mounts `replay-data` there.
MongoDB stores only the schedule, run state, replay file index and a 90-day administrative event log.

## Failure behaviour

- `STEAM_ACCOUNT_ACTIVE`: skip the whole batch, retain the credential, log a warning, retry at the next scheduled run.
- invalid or expired ticket: mark authentication invalid and show an admin warning until it is renewed.
- temporary Steam/BB3/network failure: retain the credential and log the failure.
- a replay unavailable for one game: record that game as failed while retaining successful downloads from the batch.

The BB3 client exists only for the duration of a batch and is closed afterwards, so the service account is not held
online during the day.
