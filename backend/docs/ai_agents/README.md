# AI reporter profiles

Canonical AI reporter profiles live in `backend/docs/ai_agents/reporters/`. These files
are application data loaded by the backend, not merely developer documentation.

Each profile contains YAML frontmatter for machine-readable defaults and Markdown for
persona/background/public-profile context. Portrait assets live under
`frontend/public/img/portraits/`.

Static profile data includes identity, editorial role, public biography, voice/defaults
and portrait metadata. Runtime relationships, grievances, memories, queue state,
assignments, generated content and provider/model provenance belong in persistence.

Site Admin runtime overrides may enable/disable reporter capabilities and generation
behavior without editing the canonical profile files.

Public Staff profiles must not expose provider/model/prompt/runtime configuration or
private relationship/memory state.

AI reporter ratings use the application-wide rating scale. Direct mentions/tags are
handled through the canonical interaction path when the reporter/capability is enabled;
global/admin disables still take precedence.

Match/replay/domain facts are authoritative input. Persona affects selection, emphasis
and tone but must not invent events.

For the broader identity/context/provenance model, see `../AI_ARCHITECTURE.md`.
