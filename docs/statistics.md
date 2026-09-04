# BlaskScore statistics

Statistics are selected through LeagueSystem seasons, stages and their counting rules. A match occurring in several
sources is de-duplicated, and an excluded match is not counted. Official/admin-adjusted scores are used for team
results; the stored match detail is used for CAS and player events.

## Endpoints

- `GET /league-systems/{system}/seasons/{season}/statistics` – seasonal top 10 for players and teams.
- `GET /league-systems/{system}/statistics/marathon?edition=ALL&mergeTeamsByName=false&page=0&size=25&sort=points`
  – all-time team table plus player leaderboards (up to 100 entries per category).
- `GET /user/statistics?leagueSystemId={system}` – authenticated user's teams, players and coach-versus table.

`edition` accepts `ALL`, `BB1`, `BB2` or `BB3` (and any future `GameType`). When several rulesets are combined,
ruleset-dependent player measures such as SPP, MVP and movement distances are omitted. Teams are separate by their
edition-aware BlaskScore identity unless `mergeTeamsByName=true`; merged rows retain badges for all included editions.

The personal page uses persisted Cyanide coach IDs. Opening **My teams** after a Steam login extracts the owner ID
from the BB3 team response and adds it to the BlaskScore user. The Steam session may subsequently expire or be closed.

Results are cached for ten minutes to keep the 31-season marathon responsive.
