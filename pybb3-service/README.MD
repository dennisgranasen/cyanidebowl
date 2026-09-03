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
