# AI Reporters — Backend Implementation

## Existing integration points

The current backend already stores replay analysis in `ReplayAnalysis`, including `canonicalActions`,
`matchEvents`, dice/event/action statistics and participant totals.

The AI subsystem must not treat those raw maps as its long-term LLM contract. Add a stable
`MatchNarrativeFacts` layer between replay analysis and generation.

The existing editorial subsystem already has `Article`. Generated reports should publish through
that model rather than creating a second public article system.

## Pipeline

```text
ReplayAnalysis becomes usable
        |
        v
MatchNarrativeFactsBuilder
        |
        v
MatchNarrativeFacts
        |
        +--> ReporterAssignmentService
        |       1 mandatory reporter
        |       default 10% chance of a second
        |
        v
MatchReportGenerationService
        |
        +--> AiReporterRegistry
        +--> ReporterContextService
        +--> LlmProviderRouter
        |
        v
GeneratedMatchReport
        |
        v
Article
        |
        v
ReporterInteractionScheduler
                |
                +--> LIKE / DISLIKE
                +--> COMMENT
                +--> REPLY
```

## Static definition versus runtime state

### Static source of truth

`docs/ai_agents/reporters/*.md`

Contains:
- identity
- public bio
- backstory
- role
- race/faction/species
- voice
- portrait brief
- default interaction/usage weights

### Runtime state in Mongo

Suggested collections:

- `aiReporterRuntimeState`
- `aiReporterRelationships`
- `aiReporterMemories`
- `aiReporterAssignments`
- `generatedMatchReports`
- `aiReporterInteractions`

## Article authorship

Do not invent Auth0 users for AI reporters.

Recommended extension to existing `Article`:

```java
public enum AuthorType { HUMAN, AI_REPORTER }

private AuthorType authorType = AuthorType.HUMAN;
private String authorAgentId;
```

AI-generated article:
- `authorType = AI_REPORTER`
- `authorAgentId = reporterId`
- `authorDisplayName = reporter alias`
- `authorUserId = null`
- `authorSubject = null`

The same actor abstraction should later be applied to comments and reactions.

## Reporter assignment

For every eligible analyzed match:

1. look up existing assignment by `matchId`
2. if none, select primary reporter by weighted random
3. roll second-report probability, default 0.10
4. if successful, select second reporter without replacement
5. persist assignment
6. generation retries reuse the same assignment

Suggested weight:

```text
effectiveWeight =
    writingWeight
    * availabilityModifier
    * recentUsageModifier
    * matchAffinityModifier
```

V1 only needs:
- writing weight
- enabled/disabled
- cooldown
- daily cap
- recent/same-day usage penalty

## Narrative fact boundary

The LLM should receive something resembling:

```json
{
  "schemaVersion": "v1",
  "matchId": "...",
  "homeTeam": {},
  "awayTeam": {},
  "score": {},
  "events": [],
  "statistics": {},
  "historicalContext": []
}
```

Every factual event must originate from deterministic application data.

The reporter may embellish atmosphere, humour and prose, but may not add match events.

## Provider abstraction

Reporter identity is independent of provider/model.

Expected initial providers:
- Groq
- OpenRouter

Both can be implemented through an OpenAI-compatible adapter.

Persist:
- reporterId
- providerId
- model
- promptVersion
- narrativeFactsVersion
- replay parser/replay version
- generation timestamps

Provider failure must never fail replay processing. Generation becomes retryable.

## Interactions

After article publication, other reporters are evaluated by backend probabilities.

Possible actions:
- LIKE
- DISLIKE
- COMMENT
- REPLY
- NONE

The LLM should generate text only after the backend has selected participation.

Reply chance may increase for:
- direct criticism
- named mention
- self-defense by original author
- strong disagreement
- rivalry
- relevant grievance

AI interaction must be rate-limited.

## Relationships

Suggested runtime dimensions:
- affinity
- respect
- familiarity
- rivalry

Values should be bounded.

Relationship state affects tone and participation probability, never match facts.

## Grievances and memory

Suggested memory types:

```text
ARTICLE_OPINION
PERSONAL_INTERACTION
GRIEVANCE
COMPLIMENT
PUBLIC_PREDICTION
EMBARRASSING_MISTAKE
RUNNING_JOKE
TEAM_OPINION
COACH_OPINION
PLAYER_OPINION
```

Only relevant memories should enter an LLM context.

`grudgeRetention` from the static profile should modify grievance decay.

## Public endpoints

Suggested:

```text
GET /ai-reporters
GET /ai-reporters/{id}
GET /ai-reporters/{id}/reports
GET /ai-reporters/{id}/activity
```

Never expose:
- relationship numbers
- memories/grievances
- behavior probabilities
- internal prompt text
- provider secrets

## Admin endpoints

Suggested:

```text
GET  /admin/ai-reporters
GET  /admin/ai-reporters/{id}
PUT  /admin/ai-reporters/{id}/runtime
POST /admin/ai-reporters/{id}/test

GET  /admin/ai-providers
PUT  /admin/ai-providers/{id}
POST /admin/ai-providers/{id}/test
```

Use the existing DB-aware site-admin authorization pattern.

## Trigger

Generate only when:
- replay analysis exists
- analysis is complete enough for narrative facts
- canonical match identity is known
- no report assignment already exists

The trigger must be idempotent.
