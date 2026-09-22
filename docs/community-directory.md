# Community directory

The community page requires an active league system. Navigation carries the
selected system in the URL; direct visits select the public primary system (or
the first public system). The server restricts the directory to members whose
current team appears in that system's season participation. Seasonal filtering
uses the same canonical participation data as article audiences. It does not
claim to reconstruct a fan's historical membership or past team loyalties.

Filters combine team, season, team race, member species, and current active state.
Sorting supports name, original membership creation date, visible comment count,
team, season names, race and activity, in either direction. Missing membership
dates sort last. Reactivation does not reset membership age. Active members are
shown initially; inactive and all-members options remain available.

Profile histories paginate twenty comments at a time, newest first, and preserve
inactive members' history. Deleted comments and comments on unpublished/missing
articles or match articles are excluded from both counts and histories. Links
open the existing target's comment stream through a dedicated discussion route,
and focus the selected comment. The original article/team page is linked when
available. This creates no duplicate comments or new discussion identities.

Directory counts are computed on demand, with target visibility cached per
request. Season audiences and comment histories are currently materialized on
the server; a larger installation may need indexed aggregation and cached
season membership. Frontend filtering and sorting run over the scoped directory.
