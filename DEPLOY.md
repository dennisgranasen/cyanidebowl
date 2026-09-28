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

The Firebase workflow and Oracle workflow are both started manually from GitHub Actions.
The Oracle workflow builds ARM64 images, pushes them to GHCR, and updates the VM over SSH.
It does not copy or replace the VM's runtime `.env` file.

## One-time Oracle setup

1. Create an ARM64 Oracle Linux or Ubuntu VM with a reserved public IPv4 address. Provide
   enough memory for the Java 25 backend and Docker; the workflow builds images on GitHub,
   so Maven and .NET SDKs are not required on the VM.
2. Configure the Oracle network security list/firewall to allow SSH from your trusted
   admin address and the GitHub Actions runner addresses used by this workflow. GitHub's
   hosted-runner IP ranges change; keep the allowlist current, use a static-IP runner
   option, or use a secured self-hosted runner if that is operationally preferable. Do
   not expose ports 80, 443, 8000, or 8080: the tunnel connects outbound. Ensure outbound
   HTTPS and Cloudflare Tunnel connectivity (UDP/TCP 7844, with HTTPS fallback) are
   allowed.
3. Install Docker Engine and the Docker Compose v2 plugin. Create a deployment user, add
   it to the Docker group, and install the public half of a dedicated SSH key in that
   user's `~/.ssh/authorized_keys`. Membership in the Docker group grants root-equivalent
   control of the VM.
4. Create `$HOME/cyanidebowl` for that SSH user and put the production environment file
   at `$HOME/cyanidebowl/.env`. Start from [.env.oracle.example](.env.oracle.example),
   fill in real values, and restrict the file to its owner (`chmod 600`). Do not commit it.
5. Ensure the VM has outbound access to MongoDB Atlas, GHCR, Auth0/JWKS, Cloudflare, and
   any AI providers enabled by the application. In Atlas Network Access, allow the VM's
   stable public egress IP (or use a deliberate private-network solution); do not open
   Atlas to all addresses just to make deployment work.

The `.env` values that must be set for this topology are the MongoDB URI, the Cyanide and
pybb3 internal keys, Auth0 issuer/domain/audience/client ID, frontend origin, API and AI
provider configuration, and `CLOUDFLARE_API_TUNNEL_TOKEN`. Generate fresh secrets after
rotating the values previously exposed. `PYBB3_CREDENTIAL_ENCRYPTION_KEY` must remain
stable after credentials have been encrypted; changing it can make existing stored
credentials unreadable. Back up the key securely before rotating it and follow the
application's credential re-encryption/recovery procedure if rotation is required.

The Compose file uses named Docker volumes for pybb3 credentials, replay data, and
community media. These survive container replacement but are not backups. Schedule
separate encrypted backups and test restoration.

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

## GitHub configuration

Add these repository **secrets** before running the Oracle workflow:

- `ORACLE_HOST`: reserved public IP or SSH hostname.
- `ORACLE_USER`: deployment SSH user.
- `ORACLE_SSH_PRIVATE_KEY`: private half of the dedicated deployment key.
- `ORACLE_SSH_KNOWN_HOSTS`: verified SSH host-key line(s) for the VM. Verify the
  fingerprint through a trusted Oracle console before saving it; the workflow does not
  trust a key discovered during deployment.
- `GHCR_USERNAME`: GitHub account that owns the registry read token.
- `GHCR_READ_TOKEN`: a classic personal access token with `read:packages` and access to
   both private GHCR packages. The workflow's `GITHUB_TOKEN` publishes the images; this
   token is only used temporarily by the VM to pull them.

`PYBB3_REF` is an optional repository variable; it defaults to `main`. Pin it to a commit
or release for reproducible pybb3 builds.

From **Actions → Deploy backend and pybb3 to Oracle → Run workflow**, select the ref to
deploy and run it. The workflow tags images with that commit SHA, transfers the Oracle
Compose file, pulls the ARM64 images, and waits for backend/pybb3 health checks. A failed
health check fails the deployment. The previous image tags remain in GHCR for manual
rollback; keep the matching Compose file and image SHA together when rolling back.

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

1. Run the Oracle workflow and confirm the `backend`, `pybb3-service`, and `cloudflared`
   containers are healthy/running in `$HOME/cyanidebowl` with
   `docker compose --env-file .env --env-file .deploy.env -f compose.oracle.yaml ps`.
2. Confirm `https://api.example.com/actuator/health` returns a healthy response and that
   the API's `/api` routes work through the tunnel.
3. Run the Firebase frontend workflow, then test login, API calls, CORS, and one operation
   that uses `pybb3-service`.
4. Check GitHub Actions, Cloudflare Tunnel status, Atlas access logs, and backend logs
   before switching the main frontend DNS hostname.

The Oracle workflow deploys only the backend, pybb3, and the API tunnel. Firebase Hosting
is the frontend deployment path; it does not require a GCP VM or Cloudflare Tunnel.