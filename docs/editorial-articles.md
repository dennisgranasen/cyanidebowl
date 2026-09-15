# Editorial articles

Articles have multiple typed associations: `LEAGUE_SYSTEM`, `SEASON`, `TEAM`,
`PLAYER`, `FAN`, and `STAFF`. Legacy `leagueSystemId`, `seasonId`, and `teamIds`
remain readable; saves also maintain these fields for existing AI consumers.
Free-text tags and mentions in prose do not change distribution.

## Distribution

1. Explicit seasons restrict an article to those seasons, even when a league or
   participating team is also linked.
2. Without explicit seasons, team/player articles appear in seasons containing
   those participants. The audience includes historical match rosters and stored
   participation, not just current team rosters.
3. General league articles appear in every season of that league.
4. Articles without league, season, team or player associations are global.
   Fan/staff associations alone do not restrict distribution.
5. Only published articles enter the public feeds, newest publication first.
   Relevance is evaluated before the result limit.

The home page and menu use the selected season. On entity pages, the menu shows
global news and articles directly linked to that entity. Writing from these
surfaces carries the context into the editor. Players link to their roster row
on the team page, using a `player` query parameter and an anchor.

## Editorial workflow

`/editor/articles/new` creates an article; `/editor/articles/:articleId` opens a
saved draft or article. Every authenticated human can write about any subject and
reopen their own drafts, submissions and rejected articles from **My articles**.
Only the author or an authorized editor can edit a human article; AI articles
remain editor-only.

Human submissions enter `PENDING_REVIEW` unless the author is an authorized
editor or every association references a team they currently own or a current
player on those teams. Coach ownership uses verified claims, including the game
edition. Adding any league, season, fan, staff, foreign team/player or additional
channel removes the coach exception. Historical ownership alone is insufficient.
The server computes the result even when a caller requests `PUBLISHED` directly.

A new revision is checked again. Saving a published article as a draft or a
revision requiring approval withdraws it from the public feed until it is
published/approved again; old acceptance metadata is cleared. Authors cannot
approve/reject their own submissions or edit other people's drafts. The editor includes
association search, hyperlinks, uploaded images, and generated illustrations.
It uses the existing Tiptap 2 StarterKit and image/link extensions. The toolbar
provides headings, bold/italic/strikethrough, lists, quotes, separators, links,
undo/redo, formatting cleanup, and preview. Editing and publication share the
same typography. PNG/JPEG images can be selected (including multiple files),
dropped at a text position or pasted from the clipboard; existing images can
be moved using Tiptap without duplicate uploads. Image upload and illustration
requests are available to authenticated authors, while AI-writer commissioning
and policy management remain editorial actions.
Drafts remain editable without requiring a public article URL.

The editor's AI writer section uses the existing reporter registry, effective
reporter profile, canonical context assembly, and provider router. Generated
articles default to `PENDING_REVIEW`; editors can edit, accept/publish, or reject
them. Publication invokes the existing fan and staff article notifications.

Auto-accept policies are stored in `editorialArticlePolicies`, per league or
globally. Every associated league must enable auto-accept. Explicit seasons use
their parent league's policy. Policies default to review and can only be changed
by an authorized editor/site administrator. Global AI publication additionally
requires confirmation for the request; the global policy alone is insufficient.

## Images

Uploads accept raster images, enforce a 10 MB input limit and 20 million pixels,
and re-encode to PNG. Assets use the existing persistent community-media store
and public asset endpoint. Generated illustrations use the configured
`AiCommunityImageRenderer`; the default prompt uses the article title and text,
and an explicit prompt overrides it. Image generation requires the existing
community-media provider configuration and credentials.

Text/image generation is synchronous, so failures appear in the editor while
the existing draft remains available. No external generation calls are made by
the automated tests.

## Validation

Targeted backend tests cover seasonal relevance, legacy associations, global
publication confirmation, review transitions, authorization, and auto-accept
policies. Frontend tests cover context propagation and the league season UI.

## Shared match editor and illustrations

Match article creation and editing use the same rich-text toolbar and image
controls as general articles. The toolbar has grouped icon controls, tooltips,
active states, underline and paragraph/heading alignment. Match articles store
optional sanitized `bodyHtml` alongside a derived plain-text `body`; legacy
reports are escaped when opened in the editor and remain readable without
migration. AI context consumers continue using the plain-text body.

Illustrations can be requested repeatedly, even after all body content/images
have been removed: a title, custom prompt or match reference is sufficient.
Requests reset their busy state on success and failure. A selected image can be
replaced; otherwise the new image is inserted at the cursor.

For match illustrations, `MatchArticleService.reportingContext` supplies the
same canonical MATCH_REPORT assembly, replay evidence projection and competition
history used by the reporter. The current title/text and optional visual brief
are added to the image request, without truncating the match evidence. As with
AI match reporting, an analyzed replay is required; manual image uploads remain
available without a replay. Internal reporter context is not returned to the
browser in the image response.
