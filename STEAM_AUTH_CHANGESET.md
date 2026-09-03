# Steam authentication changeset

This bundle is based on `origin/dev` at `b5a89a0`.

## Setup

1. Apply the companion pybb3 web-auth changes before building the service.
2. Generate a long random `PYBB3_INTERNAL_API_KEY` and give the same value to
   Spring and `pybb3-service`.
3. For local Docker: `docker compose --profile pybb3 up --build`.
4. Start Spring with `PYBB3_SERVICE_URL=http://localhost:8000` and
   `PYBB3_COOKIE_SECURE=false` locally. Production must use secure cookies.
5. Enable Google and GitHub connections for the Auth0 application. See
   `docs/authentication.md` for Steam sign-in, NAF and safe account linking.

The Docker build should pin `PYBB3_REF` to an immutable release or commit in
production rather than leaving it at `main`.

## Removed obsolete files

Remove these paths when applying a changed-files bundle over an existing tree:

- `backend/frontend/` (an accidental stale duplicate of the real frontend)
- `pybb3-service/pom.xml` (the Python service is not a Maven module)
- `pybb3-service/README.MD` (replaced by `README.md`)

## Verification performed

- pybb3 owner-isolation tests: 2 passed
- Python module compilation: passed
- `git diff --check`: passed

Maven is unavailable in the build environment and the installed Java runtime is
17 while this repository requires Java 25, so the Java suite must be run in the
normal project toolchain before deployment.
