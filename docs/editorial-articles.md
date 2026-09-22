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
are supplied to a text-model step together with the complete match evidence. It
selects one coherent scene and writes a compact image brief. The photographer's
full visual direction is reserved separately, and the final image prompt is
bounded to 2,048 characters for Cloudflare compatibility. If the text model
exceeds its budget, only its generated scene is shortened; raw match evidence
is never blindly truncated into an image prompt. Short general-article briefs
that already fit do not need this additional text-model call. The text step uses
the existing `EDITORIAL_ARTICLE` routing and therefore needs a configured text
model as well as an image model. As with
AI match reporting, an analyzed replay is required; manual image uploads remain
available without a replay. Internal reporter context is not returned to the
browser in the image response.

The image-description text step has a 90-second deadline, including time spent
in the AI execution queue. Its cancellation interrupts the queue wait and removes
pending work, including work waiting for a quota reset. Calls already executing
at a provider may finish under that provider's own timeout; their results do not
start image rendering after the description deadline. The browser independently
limits the complete request (including token acquisition) to five minutes and
unlocks the editor with a localized error. This client limit is not a server job
cancellation protocol. Existing requests need the updated backend/frontend to
benefit from these limits.

The image editor polls `/articles/tools/image-queue` every 15 seconds using the
selected photographer's actual EDITORIAL_ARTICLE route and priority. The public
response (authenticated writers only) contains aggregate counts, an earliest
quota restart and an advisory wait for a new request; no prompts, agent identities
or provider errors are exposed. After three successful text jobs, queue wait is
estimated from average service time, eligible jobs of equal/higher priority,
running work and concurrency. This is not a tracked job position or a guaranteed
completion time. Image-provider latency is additional and currently unknown.
Article images bypass the persisted community media queue; shared provider
quotas can still affect both. Direct requests retain their timeouts rather than
becoming durable multi-day orders.

## Editorial photographers and illustrators

The visual staff catalog contains 18 distinct photographers and illustrators in
`backend/src/main/resources/ai/photographers.json`. Each has a stable id, name,
and Swedish/English descriptions of personality, subject/composition preferences,
medium, and equipment/material quality. Add or edit profiles there and restart
the backend. The registry validates duplicate ids and incomplete descriptions at
startup. Visual staff are separate from reporters and do not enter automatic
writing assignments.

The public `GET /articles/tools/photographers` endpoint feeds both the visual
staff tab on the editorial page and the shared article image selector. Writers
remain in their own tab. The selector shows the selected person's complete
style and supports changing photographer between generation attempts.

Image requests send `photographerId`; the server resolves the trusted profile
and includes its visual direction alongside the article, custom brief, and any
match context. The same direction applies to non-match subjects. Preferences
must not invent events, and equipment quality affects visual texture rather than
the accuracy of depicted facts. Unknown ids return HTTP 400 before rendering;
older clients without an id use the first catalog entry. The image response
includes the creator's id/name, and the editor retains a credit in the image's
HTML title attribute. Uploaded images do not acquire an AI photographer credit.

Tests cover loading the bilingual catalog, selected styles in match and general
news requests, changing creator between requests, and rejecting unknown ids.
The image renderer is mocked; visual fidelity of generated artwork depends on
the configured image model and has not been tested with paid provider calls.
