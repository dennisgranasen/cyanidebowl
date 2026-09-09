# AI Reporter implementation backlog

## B-AR-001 — Static profile loading
- add package A files
- package docs as backend resources
- add SnakeYAML
- load Markdown + YAML frontmatter
- validate required fields
- reject duplicate IDs
- tests

## B-AR-002 — Runtime persistence
- Mongo documents
- repositories
- indexes
- runtime enabled/weight/probability overrides
- per-day activity counters

## B-AR-003 — Stable narrative facts
- implement `MatchNarrativeFactsBuilder`
- canonical home/away teams
- score
- player identities
- chronological key events
- stats
- historical context
- tests using known replays

## B-AR-004 — AI actor support in editorial system
- extend `Article` with `AuthorType`
- `authorAgentId`
- extend comments/reactions similarly
- preserve existing HUMAN defaults
- never create fake Auth0 identities

## B-AR-005 — Assignment
- weighted primary reporter
- 10% second reporter
- distinct second reporter
- idempotent concurrent creation
- cooldowns
- daily limits
- recent usage penalty
- statistical tests

## B-AR-006 — LLM provider layer
- provider config
- Groq
- OpenRouter
- fallback routing
- timeout
- 429 handling
- retry classification
- health/test endpoint
- secrets only through configuration/env

## B-AR-007 — Generation
- prompt builder
- persona + selected memory context
- structured JSON output
- sanitize HTML
- provenance
- retry state machine
- factual validation where deterministic validation is possible

## B-AR-008 — Publish through Article
- generated report -> Article
- LeagueSystem/Season metadata
- match reference
- draft/publish setting
- generated report retains provenance

## B-AR-009 — Public staff pages
- backend public API
- `/staff`
- `/staff/:reporterId`
- report/comment/reaction archive
- explicit AI Reporter badge

## B-AR-010 — Interaction engine
- reactions
- comments
- replies
- rebuttal bonus
- mention bonus
- self-defense bonus
- thread depth
- rate limiting

## B-AR-011 — Relationships and memories
- seed selected pre-existing relationships
- bounded updates
- grievances
- decay
- relevant memory retrieval
- running jokes

## B-AR-012 — Admin UI
- runtime enable/disable
- weights/probabilities
- provider/model bindings
- test reporter against historical replay
- retry/regenerate
- inspect provenance
