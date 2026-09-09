# AI Reporter Admin

The admin interface should expose effective state and runtime overrides.

Per reporter:

- master Enabled
- Can write reports
- Can interact
- Rates players
- writing weight
- comment probability
- reaction probability
- reply probability

The Markdown profile provides defaults. Mongo runtime overrides win when non-null.

Master disabled means:
- no new reports
- no new ratings
- no new comments/replies/reactions

Historical content remains visible.

Backend:
- `GET /admin/ai-reporters`
- `PUT /admin/ai-reporters/{id}/runtime`

Both are site-admin only.
