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
