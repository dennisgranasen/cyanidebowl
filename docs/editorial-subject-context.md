# Editorial subject context

Article writers and image generation share `EditorialSubjectContext` and the
`ArticleSubjectPicker` UI. Associations include leagues, seasons, teams, players,
fans, staff and `STAR_PLAYER`. Star player tags do not change an article's audience.
Image subjects initially inherit article associations and can then be edited for
that particular image. Match illustrations retain the existing replay evidence
and history. Every final image prompt retains the Blood Bowl world constraints.

`backend/src/main/resources/ai/starplayers.json` is a snapshot of the 63 player
markdown documents from [BBBase](https://github.com/BBBase-EHeresy-PPPub/BBBase/tree/abd69e2b68635665f7e2c94bdc69a0819bcfd962/docs/bb2025/starplayers).
The source commit and source URLs are recorded in the file. Portrait URLs come
from each document's own image reference, pinned to the same commit. Updating the
catalogue is an explicit import using `scripts/import-starplayers.py`; generation
never fetches arbitrary user-supplied context URLs.

When OpenAI image generation is configured, tagged star player portraits are sent
as real image inputs to the [image edits endpoint](https://developers.openai.com/api/reference/resources/images/methods/edit).
Reference images use the configured OpenAI image model and quality. Text-only
illustrations keep the existing default image provider. If no reference-capable
provider is configured, star player facts still enter the scene brief and the UI
reports that portraits were not used. A maximum of 16 portrait inputs is supported.

Generated images store their tags and visual brief in `editorialImageSubjects`.
The editor persists the metadata ID in the image's `data-editorial-image` HTML
attribute. Only images still present in the published body trigger tagged-image
comments. Both general articles and match articles support this behavior.

Tagged active fans with an AI identity bypass random selection and team-affinity
filters. Their comments use the existing persistent work queue, with five attempts
for provider failures, and per-image and per-fan idempotency prevents duplicate
comments. Adding another tagged image can trigger a new comment even if the fan
already commented on the article. The global AI execution switch still pauses queue execution. Deleted
images and unpublished articles are checked again before queued work runs.
Reporter image tags add the profile's mention bonus to ordinary comment
probability; they do not force a response. Reporter enablement and activity limits
still apply. Image briefs are supplied to commenters so they can react to the
illustration even though the text model cannot see the final rendered image.
