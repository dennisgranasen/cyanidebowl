# cyanidebowl / BlaskScore

BlaskScore is a Blood Bowl results, statistics and editorial site built on the original
warp-scores/cyanidebowl codebase. Development happens on the `dev` branch.

The application consists primarily of:

- `backend` — Spring Boot application and persistence/API layer
- `frontend` — React/Chakra UI
- `api` and `cyanide-api` — shared/Cyanide API modules
- `pybb3-service` — BB3 client/replay integration used by the Docker deployment

## Development

Safe local development uses the `dev` Spring profile and keeps external collection jobs
disabled unless they are explicitly needed.

Backend:

```bash
mvn spring-boot:run -Pserver -pl backend -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

For the current local configuration variables, use `.env.example` as the reference.
Do not commit `.env`, credentials, API keys, Auth0 tokens or database dumps.

## Verification

Backend:

```bash
mvn clean test -Pserver -DskipDocker -pl api,cyanide-api,backend -am
mvn clean package -Pserver -DskipDocker -pl api,cyanide-api,backend -am
```

Frontend:

```bash
cd frontend
npm ci
npm test -- --runInBand
npm run build
```

GitHub Actions validates `dev`/pull requests and builds the ARM64 deployment artifact
after the test jobs pass.

## AI reporters

Canonical reporter profiles live in `backend/docs/ai_agents/reporters/`; their short
technical documentation is in `backend/docs/ai_agents/README.md`. Portraits/avatars are
served from `frontend/public/img/portraits/`.

## Roadmap and backlog

Start with `ROADMAP.md` for the current execution order and cross-project dependencies.
`BACKLOG.md` contains the detailed implementation cards and acceptance criteria.

AI-specific architecture and sequencing live under `backend/docs/`, especially
`AI_ARCHITECTURE.md` and `AI_ROADMAP.md`. The pybb3 integration boundary is documented
in `pybb3-service/README.md`.

Temporary implementation notes and one-off patch artifacts should not be committed at
repository root.

## Disclaimer

This project is unofficial and is not affiliated with Cyanide, Nacon or Games Workshop.
Blood Bowl and related names are trademarks of their respective owners.
