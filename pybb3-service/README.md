# pybb3 service

Internal bridge between the BlaskScore Spring backend and the separately versioned
`pybb3` client. Do not expose it directly to browsers. Every non-health request requires
an internal key and a trusted owner ID supplied by Spring.

Each application user gets an isolated Steam Guard flow, helper process, `BB3Client`,
socket and request lock. Passwords and Guard codes are never kept.

## Configuration

Environment:

- `PYBB3_INTERNAL_API_KEY` — required shared internal service key;
- `PYBB3_REF` — pybb3 tag/commit pinned into the image build;
- `STEAM_HELPER_PATH` — helper executable path;
- `SESSION_TTL_SECONDS` — session lifetime, default 1800;
- `CHALLENGE_TTL_SECONDS` — Steam challenge lifetime, default 300.

Build:

```bash
docker build --build-arg PYBB3_REF=<tag-or-commit> -t blaskscore-pybb3 .
```

Production/deployment must pin `PYBB3_REF`; do not silently build from a moving upstream
branch when Cyanidebowl depends on a particular replay/timeline schema.

## Replay/timeline boundary

Detailed replay decoding and event inference belong in the upstream `pybb3` repository.
This service should expose a stable, versioned transport contract to the Spring backend.

For the narrative timeline, the upstream contract must distinguish ordinary movement
from movement that actually requires a test. A roll should carry its semantic reason,
for example dodge, rush/GFI, Tentacles or a negative trait such as Bone-head, Really
Stupid or Bloodlust. Supported special events should remain explicit rather than being
flattened into a generic `Move`.

`skill_rerolls` are distinct from generic/team rerolls. Consumers should retain
backwards compatibility for older payloads while preferring the explicit field when the
pinned pybb3 version provides it.

Unknown event kinds must be passed through as explicit unknown/provenance-bearing data;
the bridge must not guess a misleading domain event.

Replay/diagnostic export paths must preserve IP-address redaction before data is stored
or exposed outside the trusted service boundary.
