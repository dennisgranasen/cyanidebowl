# AI Match Report Generation

Every completed match with a successfully analyzed replay receives at least one AI-generated match report. A second independent report is generated with a default probability of 10%.

```yaml
match_reporting:
  minimum_reports: 1
  second_report_probability: 0.10
  distinct_reporter_required: true
  selection_mode: WEIGHTED_RANDOM
```

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
  -> persistence/publication + generation provenance
  -> optional interactions
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
