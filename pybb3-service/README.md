# pybb3 service

Internal bridge between the BlaskScore Spring backend and pybb3. Do not expose
it directly to browsers. Every non-health request requires an internal key and
a trusted owner ID supplied by Spring.

Each application user gets an isolated Steam Guard flow, helper process,
`BB3Client`, socket and request lock. Passwords and Guard codes are never kept.

Environment: `PYBB3_INTERNAL_API_KEY` (required), `STEAM_HELPER_PATH`,
`SESSION_TTL_SECONDS` (default 1800), and `CHALLENGE_TTL_SECONDS` (default 300).

Build: `docker build -t blaskscore-pybb3 .`. Pin pybb3 with
`--build-arg PYBB3_REF=<tag-or-commit>`.

## Local development with VS Code

Start Docker Desktop before launching `CyanideBowl local`. Its preparation task
builds and starts pybb3, waits for its health check, then builds the backend.
The local Compose override binds port 8000 to loopback so the host backend can
use its default `http://localhost:8000` URL.

Set `PYBB3_INTERNAL_API_KEY` in the root `.env`; both the backend and Compose
read it. Persistent Steam credentials also require
`PYBB3_CREDENTIAL_ENCRYPTION_KEY`.

To start just the bridge from the repository root:

```sh
docker compose -f compose.yaml -f compose.local.yaml up -d --build --wait pybb3-service
```

The container stays running after debugging stops. Stop it with:

```sh
docker compose -f compose.yaml -f compose.local.yaml stop pybb3-service
```

The local override mounts `REPLAY_SWEEPER_STORAGE_DIRECTORY` (default
`./data/replays`) at `/app/replays` for both containers. The VS Code backend
uses that host directory directly. Java remains responsible for replay storage.
Production Compose continues to use the named `replay_data` volume.
Old database paths are resolved by filename inside the configured directory.
When switching an existing local installation, preserve any replay files from
the old volume in this directory before recreating the containers.
