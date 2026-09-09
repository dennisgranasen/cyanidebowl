# AI Match Report Generation

Every completed match with a successfully analyzed replay receives at least one AI-generated match report. A second independent report is generated with a default probability of 10%.

```yaml
match_reporting:
  minimum_reports: 1
  second_report_probability: 0.10
  distinct_reporter_required: true
  selection_mode: WEIGHTED_RANDOM
```

Flow: replay -> deterministic analysis -> canonical facts -> reporter assignment -> LLM generation -> validation -> persistence/publication -> optional interactions.

The LLM must never invent touchdowns, casualties, blocks, fouls, passes, dice results, skills, score changes, injuries, player participation, turnovers or statistics. Atmosphere, metaphors, humor and crowd reactions may be invented.

Reporter identity must remain independent of provider/model. Persist reporter id, provider, model, prompt version, replay-analysis version and generation timestamp.
