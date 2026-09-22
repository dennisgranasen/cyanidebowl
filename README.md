# cyanidebowl / BlaskScore

BlaskScore is a Blood Bowl results, statistics, replay and editorial/community site built
on the original warp-scores/cyanidebowl codebase. Active development happens on `dev`.

## Repository structure

- `backend` — Spring Boot API, MongoDB persistence, scheduling, replay orchestration and AI/community services.
- `frontend` — React/Chakra UI.
- `api` and `cyanide-api` — shared model/API modules and Cyanide integration.
- `pybb3-service` — authenticated internal bridge to the separately versioned `pybb3` client.
- `compose.yaml` — production-style service topology for backend, frontend, pybb3 and Cloudflare Tunnel.

The canonical competition hierarchy is:

```text
LeagueSystem -> Season -> Phase -> Stage -> StageSource
```

Legacy Circuit/CircuitLeg terminology is retired.

## Development

Use `.env.example` as the configuration reference. Do not copy secrets into documentation.

Backend verification:

```bash
mvn clean test -Pserver -DskipDocker -pl api,cyanide-api,backend -am
mvn clean package -Pserver -DskipDocker -pl api,cyanide-api,backend -am
```

Frontend verification:

```bash
cd frontend
npm ci
npm test -- --runInBand
npm run build
```

Local frontend development:

```bash
cd frontend
npm install
npm run dev
```

## Runtime architecture

The normal deployment contains four services:

```text
browser
  -> frontend/nginx
      -> backend
          -> MongoDB Atlas
          -> pybb3-service -> pybb3 / Steam
          -> external AI providers

Cloudflare Tunnel -> frontend/nginx
```

Replay files and generated community media use persistent Docker volumes. `pybb3-service`
is internal-only and must not be exposed directly to browsers.

## AI and deterministic work

AI-backed work and deterministic replay work are intentionally separate.

AI work includes autonomous content jobs, provider/model execution queues and generated
community media. These may be limited by concurrency, provider quotas or retries.

Replay analysis is deterministic. It does not consume LLM quota. Replays are queued when
newly downloaded, explicitly requested for reanalysis, or produced by an older parser
version. Successful parser version and latest attempted parser version are tracked
separately so a failed current-version parse does not retry forever.

The parser version declared by the backend must match the normalized analysis version
returned by the pinned pybb3 integration. The code and contract tests are authoritative
for the actual version number; do not duplicate it in documentation.

## Identity and editorial model

Humans and AI-backed actors use the same canonical `User` identity model. Provider/model
information is generation provenance, not identity.

`Staff` is the canonical code/API/route term for public editorial identities. `Redaktion`
is only a Swedish UI translation.

Canonical AI reporter profiles live in `backend/docs/ai_agents/reporters/`. Runtime
relationships, memories, queue state and generation provenance are persisted data, not
profile-file content.

## Documentation

Read `DOCUMENTATION.md` before adding new documentation. The project deliberately keeps
the documentation surface small and avoids duplicating code/configuration details.

- `FEATURES.md` - concise product capabilities beyond the Warp Scores foundation.
- `ROADMAP.md` — broad remaining direction.
- `BACKLOG.md` — active actionable work only.
- `CHANGELOG.md` — implemented user/developer-visible changes.
- `backend/README.md` — backend/runtime architecture and operational boundaries.
- `backend/docs/AI_ARCHITECTURE.md` — canonical AI identity/context/provenance contract.
- `backend/docs/ai_agents/README.md` — reporter profile format and runtime overrides.
- `pybb3-service/README.md` — backend-to-pybb3 boundary.

## Disclaimer

This project is unofficial and is not affiliated with Cyanide, Nacon or Games Workshop.
Blood Bowl and related names are trademarks of their respective owners.
