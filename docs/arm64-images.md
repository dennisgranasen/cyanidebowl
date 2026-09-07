# ARM64 image artifacts

The `Test and build ARM64 images` GitHub Actions workflow runs the backend and
frontend test suites for pushes and pull requests targeting `dev`.

Pull requests run tests only. For a push to `dev`, the workflow builds the three
ARM64 application images only after both test jobs succeed:

- `cyanidebowl-backend:arm64`
- `cyanidebowl-frontend:arm64`
- `cyanidebowl-pybb3:arm64`

The images are exported with `docker save` to `cyanidebowl-arm64.tar.gz` and
uploaded as a GitHub Actions artifact. Nothing is pushed to a container registry.

The artifact is retained for 14 days and also contains a SHA-256 checksum.

## GitHub repository variables

Configure these under **Settings → Secrets and variables → Actions → Variables**:

- `REACT_APP_BACKEND_URI`
- `AUTH0_DOMAIN`
- `AUTH0_CLIENT_ID`
- `AUTH0_AUDIENCE`
- `PYBB3_REF` (optional; defaults to `main`)

The frontend values are build-time configuration and become part of the browser
bundle. They are not application runtime secrets.

## Runtime secrets

Keep production runtime secrets in the `.env` file on the Raspberry Pi. They
are not required by GitHub Actions merely to build the images.

Examples include:

- `SPRING_MONGODB_URI`
- `CYANIDE_API_KEY`
- `PYBB3_INTERNAL_API_KEY`
- `PYBB3_CREDENTIAL_ENCRYPTION_KEY`
- `CLOUDFLARE_TUNNEL_TOKEN`

No manually configured GitHub Actions secret or container-registry credential
is required by this workflow.

## Deploying the artifact to Raspberry Pi

Download the `cyanidebowl-arm64-<commit SHA>` artifact from the successful
GitHub Actions run and copy `cyanidebowl-arm64.tar.gz` to the Raspberry Pi.

Optionally verify it:

```bash
sha256sum -c cyanidebowl-arm64.tar.gz.sha256
```

Load all three images:

```bash
gunzip -c cyanidebowl-arm64.tar.gz | docker load
```

The images retain the local tags used by `compose.rpi.yaml`, so no compose
changes are required.

Restart the application using the newly loaded images:

```bash
docker compose -f compose.yaml -f compose.rpi.yaml up -d --no-build
```


## Optional host-side automatic deployment

Automatic deployment is not installed or managed by this repository. A Raspberry Pi
or other deployment host may optionally run a local updater that checks the latest
successful `docker-arm64.yml` run, compares it with the last deployed run, downloads
the artifact only when it changed, verifies the checksum, loads the images and runs
Compose.

This host-local automation needs:

- GitHub CLI (`gh`) authenticated with read access to the repository and Actions
  artifacts;
- permission for the service user to run Docker without `sudo`;
- the repository deployment files and production `.env` already present on the host;
- a writable state directory for the last successfully deployed workflow run ID.

For example, a host-local `/usr/local/sbin/update-cyanidebowl` can follow this pattern:

```bash
#!/usr/bin/env bash
set -euo pipefail

REPO="dennisgranasen/cyanidebowl"
WORKFLOW="docker-arm64.yml"
BRANCH="dev"
DEPLOY_DIR="/opt/docker/cyanidebowl"
STATE_DIR="/var/lib/cyanidebowl-updater"
STATE_FILE="${STATE_DIR}/last-run-id"

mkdir -p "${STATE_DIR}"

run_id="$(
  gh run list     --repo "${REPO}"     --workflow "${WORKFLOW}"     --branch "${BRANCH}"     --status success     --limit 1     --json databaseId     --jq '.[0].databaseId'
)"

[[ -n "${run_id}" ]] || exit 0

last_run_id=""
[[ -f "${STATE_FILE}" ]] && last_run_id="$(cat "${STATE_FILE}")"
[[ "${run_id}" != "${last_run_id}" ]] || exit 0

tmpdir="$(mktemp -d)"
trap 'rm -rf "${tmpdir}"' EXIT

gh run download "${run_id}"   --repo "${REPO}"   --pattern 'cyanidebowl-arm64-*'   --dir "${tmpdir}"

artifact_dir="$(find "${tmpdir}" -mindepth 1 -maxdepth 1 -type d | head -n1)"
cd "${artifact_dir}"
sha256sum -c cyanidebowl-arm64.tar.gz.sha256
gunzip -c cyanidebowl-arm64.tar.gz | docker load

cd "${DEPLOY_DIR}"
docker compose -f compose.yaml -f compose.rpi.yaml up -d --no-build

echo "${run_id}" > "${STATE_FILE}"
```

A matching oneshot service can run as the normal deployment user:

```ini
[Unit]
Description=Update Cyanidebowl from latest successful GitHub Actions build
After=network-online.target docker.service
Wants=network-online.target
Requires=docker.service

[Service]
Type=oneshot
User=dennis
ExecStart=/usr/local/sbin/update-cyanidebowl
```

For a conservative default, check once per day rather than continuously:

```ini
[Unit]
Description=Check daily for a new Cyanidebowl ARM64 artifact

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

The exact service user, deployment path and check frequency are host policy, not
repository configuration. Do not put production secrets or a GitHub token in the
repository. The updater should record the workflow run ID only after checksum
verification, `docker load` and `docker compose ... up` have all succeeded.
