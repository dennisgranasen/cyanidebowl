# Article generation context

AI article generation uses the existing provider abstraction:

```text
ArticleGenerationContext
  -> ArticleGenerationPromptFactory
  -> LlmProvider.LlmRequest
  -> configured LlmProvider
```

The context is provider-neutral and serialized as one versioned JSON document in the
user prompt. This means the same context works with any language-model API implemented
behind `LlmProvider` and with the existing reporter/model routing.

## Source hierarchy

1. `matchFacts` is authoritative for events and match outcome.
2. Structured competition, roster, form, streak, record and rivalry data is factual context.
3. `relatedArticles` is editorial context. Human/coach-authored claims are attributed opinions.
4. Reporter profile and an optional editorial brief control angle and voice only.
5. None of the contextual layers may override or invent match facts.

## Context layers

`EditorialContext` is intentionally compositional:

- `competition`: standings, phase/stage, playoff/elimination significance
- `teamForm`: recent results and deterministic team-form signals
- `playerSignals`: streaks, records, record chases and milestones
- `relatedArticles`: previews, coach reports and earlier coverage
- `rivalry`: coach-vs-coach, team-vs-team and race-vs-race history over the LeagueSystem
- `rosterAvailability`: injuries/MNG and other availability information

Every major context object has an `extensions` map. New deterministic context builders can
therefore add data without changing the language-model provider contract.

## Article types

The generation request carries an article type rather than encoding pre/post-match semantics
in the reporter implementation. Initial useful values are:

- `PRE_MATCH`
- `MATCH_REPORT`
- `POST_MATCH_ANALYSIS`
- `COLUMN`
- `NEWS`

Both humans and AI reporters may author the same article types; authorship and article type
are separate concepts.
