# AI Match Report Generation

## Editorial publication

Generated prose and editorial publication are separate concerns. An editor/site-admin
may explicitly ask an enabled AI reporter for an article only when a `ReplayAnalysis`
exists for the match. The generated text becomes a `MatchArticle` with
`PENDING_REVIEW`; it is never published directly by the generation pipeline.

See `MATCH_ARTICLES.md` for authorization, review states and UI/API behavior.

### Current behavior vs target automation

The implemented safe path is explicit editor/site-admin generation: a caller chooses an
enabled reporter, the server requires analyzed replay context, and the result is stored
as `PENDING_REVIEW`.

Automatic coverage of completed matches is a **target policy for B-016**, not a guarantee
of the current generation pipeline. When autonomous scheduling is implemented, the
intended default policy is at least one generated report for an eligible completed match
and an optional second independent report with a default probability of 10%:

```yaml
match_reporting:
  minimum_reports: 1
  second_report_probability: 0.10
  distinct_reporter_required: true
  selection_mode: WEIGHTED_RANDOM
```

That policy must still honor reporter/global enablement, quotas, budgets, retries,
idempotency and kill switches before it may create review candidates.

Canonical flow:

```text
replay/data
  -> deterministic analysis and in-world domain projection
  -> canonical context retrieval
  -> ContextPlanner / ContextAssembler
  -> reporter assignment
  -> CanonicalLlmRequest
  -> configured LlmProvider
  -> CanonicalLlmResponse
  -> semantic/output validation
  -> persistence as reviewable editorial content + generation provenance
  -> explicit editorial publication/rejection
  -> optional reporter interactions
```

The LLM must never invent touchdowns, casualties, blocks, fouls, passes, score changes,
injuries, player participation, turnovers or statistics. Atmosphere, metaphors, humor
and crowd reactions may be invented when they do not contradict domain facts.

Internal mechanical/replay terminology is not part of the reporter's ontology. It must
be projected into observable in-world sporting events before narrative generation.

Reporter identity remains independent of provider/model. Persist canonical author user
id plus generation mode, agent id/version, provider, model, prompt/context-profile
version, provider request id, token usage, source revision and generation timestamp when
available.

Provider execution also writes bounded generation traces for observability/audit. Trace
retention is operational metadata, not durable reporter memory. Reporter reactions to
articles/comments are semantic persona decisions through canonical context; the
interaction policy decides whether an interaction occurs, while reaction choice decides
its POW/SKULL direction and strength.
