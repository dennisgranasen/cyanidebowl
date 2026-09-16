# BlaskScore backend

Spring Boot backend for the BlaskScore application.

This README documents runtime boundaries and operational concepts. Class-level behavior,
environment defaults and API details should normally be read from code, tests and
`.env.example` rather than duplicated here.

## Responsibilities

The backend owns:

- public/admin HTTP APIs and authorization;
- MongoDB persistence;
- LeagueSystem/season/phase/stage/source orchestration;
- match, standings, bracket and editorial/community domain services;
- replay artifact storage and deterministic reanalysis orchestration;
- AI identity/context/provenance, admission, scheduling and community-media orchestration;
- the authenticated client boundary to `pybb3-service`.

## External boundaries

### MongoDB

MongoDB is the durable store for application/domain state, queue state that must survive
restart, generation traces and replay metadata. Binary replay artifacts and generated
community images are stored on filesystem/Docker volumes rather than in MongoDB.

### pybb3-service

`pybb3-service` is an internal authenticated service. It owns Steam/BB3 client interaction
and delegates replay decoding/parsing to the pinned `pybb3` client.

The backend stores the returned normalized analysis. Its parser-version constant must
match the analysis version emitted by the pinned integration. Version pins belong in code
and build configuration; this document deliberately does not repeat the numeric version.

### AI providers

Provider/model selection is execution provenance, not user identity. Provider-specific
configuration belongs in `.env.example` and Spring configuration.

## Work queues

BlaskScore has several different work mechanisms. They must not be conflated.

### Autonomous AI work

Durable application-level candidate/work queue. It represents content/community work that
may survive process restarts and can be reprioritized or removed by Site Admin.

### AI target execution queues

Per-target provider/model execution queues. They handle concurrency, retries, quota blocks
and provider backpressure. These queues are process-local; durable history comes from
generation traces rather than the queue itself.

### Community media queue

Durable queue for generated profile/avatar/media assets. Renderer choice is an execution
detail and must not leak into canonical community identity.

### Replay analysis queue

Deterministic and independent of all LLM quotas.

A replay is eligible when it is newly downloaded/unanalysed, explicitly requested for
manual reanalysis, or successfully analysed by an older parser version.

Replay metadata distinguishes:

- `parserVersion` — version of the last successfully stored analysis;
- `analysisAttemptVersion` — parser version most recently attempted;
- `analysisRequestedAt` / `analysisRequestedBy` — explicit manual request metadata.

That distinction prevents a replay that fails on the current parser version from being
retried forever while still allowing it to become eligible after a future parser-version
bump.

## Replay storage

The backend and `pybb3-service` share the persistent replay volume in Compose. Historical
database records may contain older absolute Docker/Windows paths; artifact resolution uses
the stored basename within the configured replay storage directory.

Do not log or export replay IP addresses.

## Verification

From the repository root:

```bash
mvn clean test -Pserver -DskipDocker -pl api,cyanide-api,backend -am
mvn clean package -Pserver -DskipDocker -pl api,cyanide-api,backend -am
```

For container verification:

```bash
docker compose build backend frontend
docker compose up -d
docker compose ps
```

Use the health endpoints and container logs to diagnose startup ordering before debugging
application behavior.

## AI documentation

Keep detailed AI documentation limited to stable contracts:

- `docs/AI_ARCHITECTURE.md` — identity, context, provenance and world-model invariants.
- `docs/ai_agents/README.md` — canonical reporter profile format and runtime overrides.
- provider documents — integration constraints where they are not obvious from code.
- `docs/MATCH_ARTICLES.md` — match-article lifecycle while that workflow remains non-trivial.

Do not add another AI roadmap; use the root `ROADMAP.md` and `BACKLOG.md`.
