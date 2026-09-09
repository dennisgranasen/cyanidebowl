# AI foundation + staff/admin UI

Target: CyanideBowl `dev`, after B/B2 files have been copied in.

This package implements the next requested increment:

1. feature-flag-safe unfinished rating generation
2. reporter profile loading + startup count logging
3. concrete `MatchNarrativeFactsBuilder`
4. concrete `PlayerRatingFactsBuilder`
5. public `/staff`
6. public `/staff/:reporterId`
7. site-admin `/admin/ai-reporters`
8. master/capability toggles in admin UI

## Install

Extract this ZIP into the repository root, preserving paths.

Then run:

```powershell
.\install-ai-foundation.ps1
```

The script is idempotent and edits only:
- `backend/pom.xml`
- `SecurityConfiguration.java`
- `frontend/src/App.jsx`
- `frontend/src/components/misc/Menu.jsx`

It does not use `git apply`.

## Build

```powershell
mvn -Pdev -pl backend -am package -DskipTests -DskipDocker=true
```

Frontend:

```powershell
cd frontend
npm test -- --watchAll=false
```

## Expected startup log

You should see:

```text
Loaded 40 AI reporter profiles
```

If you see zero profiles, check that the Maven resource entry copied
`docs/ai_agents` to `classpath:/ai_agents`.

## Facts implementation

`DefaultMatchNarrativeFactsBuilder`:
- canonical Match from `MatchRepository.findFirstByMatchId`
- teams/races/coaches/scores from `Match`
- normalized event stream from replay `matchEvents`, falling back to `canonicalActions`
- aggregate replay statistics

`DefaultPlayerRatingFactsBuilder`:
- canonical players from `Match.teams[].players[]`
- team/race/position
- player stats
- replay participant totals when identity/name can be matched
- deterministic objective baseline score

The objective score is an anchor, not the AI rating. Persona bias is allowed to disagree with it.

## Objective rating baseline v1

Starts at 5.0 and adds modest deterministic contributions for:
- TD
- CAS
- KO
- passes
- interceptions
- blocks
- fouls
- successful dodges/rushes
- MVP

and small penalties for removals/death.

This formula should be treated as versioned application logic and may be tuned later.

## Public UI

- `/#/staff`
- `/#/staff/{reporterId}`

Only runtime-active agents appear in the staff list.
Direct profile URLs remain readable and show `Inactive` when disabled.

## Admin UI

- `/#/admin/ai-reporters`

Requires `writeSiteAdmin`.

Controls:
- Enabled
- Can write reports
- Can interact
- Rates players
- Writing weight
- Reset to Markdown defaults

Disabling an agent affects future activity only. Historical content is retained.

## Still intentionally not implemented

- Groq/OpenRouter calls
- actual `PlayerRatingGenerator`
- automatic replay-complete trigger
- article generation
- interaction generation
- user-facing rating table on match pages

Those are the next vertical slice after these foundations are verified.
