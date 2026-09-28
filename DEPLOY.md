# Deployment

The production layout is a static frontend on Firebase Hosting and two application
containers on an Oracle ARM VM. A dedicated Cloudflare Tunnel exposes only the backend;
`pybb3-service` remains on the private Compose network. Oracle uses the standalone
`compose.oracle.yaml`, which deliberately contains no frontend service or build steps.

```text
Browser -> Firebase Hosting -> https://api.<your-domain>/api
                                  -> Cloudflare Tunnel
                                     -> backend:8080 -> pybb3-service:8000
```

The Firebase workflow deploys the frontend when manually started from GitHub Actions.
The Oracle workflow only builds ARM64 images and uploads a checksummed Actions artifact;
it does not connect to Oracle or publish images to a registry. You download the artifact
and deploy it yourself from a trusted computer using your own SSH connection.

## One-time Oracle setup

1. Create an ARM64 Oracle Linux or Ubuntu VM with a reserved public IPv4 address. Provide
   enough memory for the Java 25 backend and Docker; the workflow builds images on GitHub,
   so Maven and .NET SDKs are not required on the VM.
2. Configure the Oracle network security list/firewall to allow SSH only from your
   trusted admin address. GitHub Actions does not SSH to the VM. Do not expose ports 80,
   443, 8000, or 8080: the tunnel connects outbound. Ensure outbound HTTPS and Cloudflare
   Tunnel connectivity (UDP/TCP 7844, with HTTPS fallback) are allowed.
3. Install Docker Engine and the Docker Compose v2 plugin. You can use the existing
   non-root OCI login (commonly `ubuntu` or `opc`); a separate deployment user is
   optional. Install your SSH public key in that user's `~/.ssh/authorized_keys` and give
   it Docker access. Membership in the Docker group grants root-equivalent control of
   the VM, so protect that account and its SSH key accordingly.
4. Use your existing `$HOME/blaskscore` directory. Ensure its `data/replays` and
   `data/community-media` directories exist, and put the production environment file at
   `$HOME/blaskscore/.env`. Start from [.env.oracle.example](.env.oracle.example), fill
   in real values, and restrict the file to its owner (`chmod 600`). Do not commit it.
   The pybb3 container runs as UID/GID `65532`. For shared replay-directory access,
   create a dedicated host group using the `PYBB3_DATA_GID` value from `.env` (default
   `20000`). First check that the GID is unused with `getent group 20000`. If it is free,
   run:

   ```bash
   sudo groupadd --gid 20000 pybb3-data
   sudo usermod -aG pybb3-data ubuntu
   sudo chown ubuntu:pybb3-data "$HOME/blaskscore/data/replays"
   sudo chmod 2770 "$HOME/blaskscore/data/replays"
    sudo setfacl -m g:pybb3-data:rwx,d:g:pybb3-data:rwx \
       "$HOME/blaskscore/data/replays"
   ```

   Replace `ubuntu` with your actual login name if different, and log out/in so your
   shell picks up the new group. Compose adds GID `20000` to pybb3 as a supplementary
   group; the directory's setgid bit and default ACL keep new entries group-accessible.
   Install the `acl` package if `setfacl` is not available. If `20000` is already
   allocated, choose a free GID and set the same value in `.env` before starting Compose.
   These commands change only directory permissions/ACLs, not existing replay files;
   inspect existing contents before changing any file ownership.
5. Ensure the VM has outbound access to MongoDB Atlas, Auth0/JWKS, Cloudflare, and any AI
   providers enabled by the application. In Atlas Network Access, allow the VM's stable
   public egress IP (or use a deliberate private-network solution); do not open Atlas to
   all addresses just to make deployment work.

The `.env` values that must be set for this topology are the MongoDB URI, the Cyanide and
pybb3 internal keys, Auth0 issuer/domain/audience/client ID, frontend origin, API and AI
provider configuration, and `CLOUDFLARE_API_TUNNEL_TOKEN`. To show Auth0 names and email
addresses in the admin user-permissions panel, configure `AUTH0_MGMT_CLIENT_ID` and
`AUTH0_MGMT_CLIENT_SECRET` for a confidential machine-to-machine application authorized
for the Auth0 Management API with only the `read:users` scope. These credentials are
backend-only; do not add them to frontend build variables or GitHub Actions. The panel
continues to use locally stored profile details if these optional credentials are unset
or Auth0 is unavailable. Generate fresh secrets after
rotating the values previously exposed. `PYBB3_CREDENTIAL_ENCRYPTION_KEY` must remain
stable after credentials have been encrypted; changing it can make existing stored
credentials unreadable. Back up the key securely before rotating it and follow the
application's credential re-encryption/recovery procedure if rotation is required.

Replay files and community media are bind-mounted from
`$HOME/blaskscore/data/replays` and `$HOME/blaskscore/data/community-media`, preserving
the existing host directories across container replacement. pybb3 credentials use a
persistent named Docker volume. None of these are backups; schedule separate encrypted
backups and test restoration.

## Cloudflare Tunnel

1. In Cloudflare Zero Trust, create a **separate remotely managed tunnel for the API**.
   Do not reuse a tunnel token still serving the old frontend.
2. Add a public hostname such as `api.example.com` to that tunnel and set its service to
   `http://backend:8080`. The `backend` hostname resolves on the shared Docker network.
   Keep the path unchanged so frontend requests to `/api/...` reach the backend.
3. Save that tunnel's token as `CLOUDFLARE_API_TUNNEL_TOKEN` in the Oracle VM `.env`.
   No inbound Oracle application port or separate host Nginx is needed.
4. After both sites are verified, repoint the frontend hostname from any old tunnel to
   Firebase Hosting and retire the old frontend tunnel connector if it is no longer used.

## Build and manual Oracle deployment

No Oracle SSH key, host, known-host entry, or GHCR token is stored in GitHub Actions.
The only optional repository variable for the image build is `PYBB3_REF`; it defaults to
`main`. Pin it to a commit or release when you need a reproducible pybb3 build.

1. Open **Actions → Build Oracle ARM64 images → Run workflow**, select the code ref, and
   start the workflow. It builds the backend and pybb3 images without pushing them to a
   registry, then uploads an artifact containing the images, checksum, and Oracle Compose
   file. Artifacts are retained for 14 days.
2. Download and extract that run's `cyanidebowl-oracle-arm64-<commit>` artifact on your
   trusted admin computer. Verify it there:

   ```bash
   sha256sum -c cyanidebowl-oracle-arm64.tar.gz.sha256
   ```

3. Copy the archive and Compose file to the VM using your own SSH key and the SSH source
   IP allowed by the Oracle firewall. For example:

   ```bash
    scp cyanidebowl-oracle-arm64.tar.gz \
       cyanidebowl-oracle-arm64.tar.gz.sha256 compose.oracle.yaml \
       USER@ORACLE_HOST:blaskscore/
   ```

4. Connect to the VM and load the images. Run the commands from the deployment user's
   home directory, where `.env` contains the runtime settings and image tags from
   `.env.oracle.example`:

   ```bash
   cd "$HOME/blaskscore"
   sha256sum -c cyanidebowl-oracle-arm64.tar.gz.sha256
   docker load -i cyanidebowl-oracle-arm64.tar.gz
   docker compose --env-file .env -f compose.oracle.yaml up -d --no-build --wait --wait-timeout 300
   docker compose --env-file .env -f compose.oracle.yaml ps
   ```

The Oracle workflow only prepares images; container deployment happens only when you run
these commands. Keep the downloaded artifact for rollback, because loading the next
artifact replaces the local `:arm64` image tags. GHCR publishing and automated pull-based
deployment can be added later, without giving GitHub Actions SSH access to the VM.

## Firebase and Auth0

The frontend workflow is also manually triggered in Actions. Configure these repository
variables for it:

- `FIREBASE_PROJECT_ID`, and optionally `FIREBASE_HOSTING_SITE` for a non-default site.
- `REACT_APP_BACKEND_URI`, set to `https://api.example.com/api`.
- `AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`, and `AUTH0_AUDIENCE`.

Add the Firebase service-account JSON as the `FIREBASE_SERVICE_ACCOUNT` Actions secret.
Grant that service account the Firebase Hosting deployment permissions required by the
Firebase CLI. The JSON and all server-side provider keys must never be frontend build
variables; React build configuration is public in the delivered JavaScript.

In Auth0, add the Firebase custom domain to the SPA's Allowed Callback URLs, Allowed Web
Origins, and Allowed Logout URLs. Also add it to the existing frontend domain's allowed
origins if both will be used. Set the Oracle `.env` `FRONTEND_URI` to the deployed
frontend origin. Keep `AUTH0_AUDIENCE` identical between the frontend build and backend
unless the Auth0 API identifier itself is intentionally changed.

## First-deploy checks

1. After the manual Oracle commands, confirm the `backend`, `pybb3-service`, and
   `cloudflared` containers are healthy/running with
   `docker compose --env-file .env -f compose.oracle.yaml ps`.
2. Confirm `https://api.example.com/actuator/health` returns a healthy response and that
   the API's `/api` routes work through the tunnel.
3. Run the Firebase frontend workflow, then test login, API calls, CORS, and one operation
   that uses `pybb3-service`.
4. Check GitHub Actions, Cloudflare Tunnel status, Atlas access logs, and backend logs
   before switching the main frontend DNS hostname.

The Oracle workflow builds images only; it does not deploy containers. Firebase Hosting
is the frontend deployment path and does not require a GCP VM or Cloudflare Tunnel.