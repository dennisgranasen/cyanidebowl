# Documentation policy

BlaskScore intentionally keeps documentation smaller than the implementation.

The goal is to document **stable boundaries, invariants, operating procedures and active
work**, not every class, setting or historical design discussion. If a value can be read
directly from code or `.env.example`, it normally should not be copied into prose.

## Sources of truth

| Topic | Source of truth |
| --- | --- |
| Current behavior | Code and tests on `dev` |
| Environment variables and defaults | `.env.example`, Spring configuration and Compose |
| Container topology and volumes | `compose.yaml` plus platform overrides |
| Broad project direction | `ROADMAP.md` |
| Actionable engineering work | `BACKLOG.md` |
| Implemented changes | `CHANGELOG.md` |
| Backend/runtime boundaries | `backend/README.md` |
| AI identity/context/provenance invariants | `backend/docs/AI_ARCHITECTURE.md` |
| AI reporter profile format | `backend/docs/ai_agents/README.md` |
| pybb3 service boundary | `pybb3-service/README.md` |

## Rules

1. Do not create a new roadmap for a subsystem. Add broad sequencing to `ROADMAP.md`.
2. Do not keep completed implementation cards in `BACKLOG.md`; move notable outcomes to
   `CHANGELOG.md` and delete the card.
3. Do not duplicate environment defaults in prose unless the default itself is a design
   contract. Link to `.env.example` instead.
4. Do not document parser/model/provider version numbers in multiple places. Keep version
   pins close to the code/build configuration that enforces them.
5. Prefer one canonical architecture document per subsystem. Small feature documents are
   justified only when they describe a stable external contract or operational procedure.
6. Temporary debugging notes, generated patches and one-off handoff files do not belong
   in the repository.
7. When implementation and documentation disagree, fix the owning document in the same
   change rather than adding another explanatory document.

## Existing detailed documents

`backend/docs/` still contains several focused references. They are useful when they
describe a real contract, but they should not grow into parallel product specifications.

Provider-specific documents should explain integration constraints, not repeat all
configuration keys. Feature documents such as match-article authorization may remain
while that lifecycle is non-obvious; if the code becomes self-explanatory, prefer
removing the document rather than preserving it indefinitely.

Reporter persona content is an exception: the Markdown files under
`backend/docs/ai_agents/reporters/` are application data loaded at runtime, not ordinary
developer documentation.
