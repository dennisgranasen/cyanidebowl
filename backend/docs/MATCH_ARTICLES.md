# Match articles

Match articles are the shared editorial primitive for human and AI-authored long-form
coverage attached to a match. They are displayed in the **Artiklar** tab of the match
modal.

This feature deliberately follows the canonical AI architecture: AI authors are users,
coach is a contextual relationship, and generation provenance is separate from editorial
classification.

## Article kinds

`EDITORIAL`
: Blödareblaskan editorial material. Human editor/site-admin articles and AI reporter
  drafts use this kind.

`TEAM_REPORT`
: A report authored by a user whose claimed coach identity is one of the two coaches in
  the match. `teamId` and `teamName` are persisted. The UI must identify it explicitly as
  `Lagrapport · <team>` and must not present it as Blödareblaskan copy.

`COACH_CONTRIBUTION`
: A match article written by a claimed coach who did not coach either side in this match.
  It requires editorial approval before publication.

## Review policy

| Author/context | May write | May publish without approval | Editorial identity |
| --- | --- | --- | --- |
| technician/site-admin | yes | yes | Blödareblaskan |
| editor (global or LeagueSystem-scoped) | yes | yes | Blödareblaskan |
| participating match coach | yes | yes | team report |
| other claimed coach | yes | no | coach contribution |
| AI reporter requested by editor/admin | generated draft | no | Blödareblaskan after approval |
| ordinary authenticated user | no | no | n/a |

The trusted-coach decision is contextual and uses the existing game-aware coach claims:
the current user's claimed coach ids are compared with `match.coaches[0..1]`; the team at
the same index is the report's team.

## Lifecycle

`DRAFT`
: Human author is still editing.

`PENDING_REVIEW`
: AI output or a non-participating coach contribution waiting for an editor.

`PUBLISHED`
: Visible to everyone.

`REJECTED`
: Refused by an editor. It remains persisted for provenance/audit purposes.

Editors can edit any match article and explicitly publish or reject review candidates.
AI output is always created as `PENDING_REVIEW`; it is never auto-published.

## AI generation

AI generation is available only to users with editor permission for the match's
LeagueSystem (site admin is included by `UserPermissionService`).

The server, not the client, enforces the replay prerequisite:

```text
ReplayAnalysisRepository.existsById(matchId) == true
```

If no analyzed replay exists, the AI request is rejected even if a caller bypasses the
UI.

Generation reuses AI-008:

```text
match
  -> SubjectRef(MATCH, matchId)
  -> ContextPlanner(EDITORIAL_ARTICLE)
  -> ContextAssemblyService
  -> ArticleGenerationLlmRequestFactory
  -> LlmExecutionService
  -> MatchArticle(PENDING_REVIEW)
```

Reporter enablement is resolved through `AiReporterEffectiveProfileService`, including
runtime `reportsEnabled` overrides. Provider id, model, provider request id and token
usage are persisted on the resulting match article.

## API

```text
GET  /matches/{matchId}/articles
GET  /matches/{matchId}/articles/capabilities
POST /matches/{matchId}/articles
PUT  /matches/{matchId}/articles/{articleId}
POST /matches/{matchId}/articles/{articleId}/submit
POST /matches/{matchId}/articles/{articleId}/publish
POST /matches/{matchId}/articles/{articleId}/reject
POST /matches/{matchId}/articles/ai
```

Anonymous readers receive published articles only. Editors receive all articles for the
match. Other authenticated authors additionally receive their own drafts/reviewed items.

The capabilities endpoint is authoritative for UI affordances. The UI must not infer
editor/coach/replay permissions independently.
