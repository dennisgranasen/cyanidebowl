# AI Player Ratings

## Rule

Every active AI agent with `capabilities.player_ratings=true` rates every participating player
in every match for which usable replay analysis exists.

This is independent of article assignment. A match may have one or two report authors while
all active rating agents score its players.

## Cost model

Generate one rating request per reporter per match, containing all participating players.
Do not perform one LLM call per player.

## Scale

Default: 1.0–10.0 in 0.5 increments.

Store both:
- deterministic `objectiveScore`
- subjective reporter `rating`

The objective score is application-owned and must not contain persona bias.

## Bias

Bias is intentional and is part of the fictional reporter voice.

Most agents have modest own-race affinity. `own_race_expectation` is separate: high expectations
can make a reporter harsher toward its own race when the player fails technically.

Example: Aeltharion Quill likes High Elves but considers a failed routine pass humiliating and
may score the player well below the objective performance score.

Bias changes judgment only. It must never alter match facts.

## Visibility

`WarpScoresUser.showAiPlayerRatings` is a presentation preference.

Ratings are still generated and persisted when a user hides them.

UI should expose individual reporter ratings, not only consensus, because disagreement is a
core part of the feature.

Suggested:
- objective score
- AI consensus
- range
- expandable per-reporter ratings/verdicts
