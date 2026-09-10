# AI reporters

AI reporter profiles are canonical Markdown files in `backend/docs/ai_agents/reporters/`.
Maven copies that directory to the backend classpath and `AiReporterProfileLoader` loads
the profiles on startup.

Each profile contains YAML frontmatter for machine-readable behaviour and a Markdown body
for persona/background text. Portrait assets live in `frontend/public/img/portraits/`.

Minimal portrait configuration:

```yaml
portrait:
  image: /img/portraits/example_full.png
  avatar: /img/portraits/example_small.png
  prompt_key: example
```

`image` is used on the full staff profile. `avatar` is used in staff/admin lists and falls
back to `image` when omitted.

## Runtime overrides

Profile files define defaults. Site admins can override these at runtime without editing
the Markdown files:

- enabled
- report generation
- interactions
- player ratings
- writing weight

Bulk overrides are available under `/admin/ai-reporters`. On `/staff/:reporterId`, a
site admin gets a settings gear for the same per-reporter controls.

## Public versus internal text

The public staff profile renders the Markdown body but excludes `## LLM guidance` and
`## Portrait brief`. Those sections remain part of the canonical agent definition.

## Ratings and interaction rules

AI reporter player ratings use integer values from **-3 to +3**. The scale is global;
legacy per-profile `scale_min`, `scale_max` and `step` values are ignored by the loader.

Interactions with human-authored articles/comments use deliberately low probabilities.
A direct tag/mention is handled as mandatory textual work when the reporter and relevant
capability are enabled; hard admin/global disables still take precedence.

Match/replay facts are authoritative input. Persona bias may change selection, emphasis,
tone and rating, but must not invent match events.
