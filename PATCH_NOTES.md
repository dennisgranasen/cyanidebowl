# What this patch changes

Based on `dev` as reviewed 2026-09-10:

- keeps the `/staff` grid free of admin Edit buttons
- replaces the remaining profile-page Edit button with an admin-only settings gear
- adds the missing `AiReporterApi.adminReporter()` call used by that gear
- uses the existing shared `AiReporterRuntimeControls`
- leaves the compact `/admin/ai-reporters` table in place
- relies on the already-present admin DTO fields `portraitImage` / `avatarImage`
- restores the repository root `README.md`, which had been overwritten by temporary
  patch instructions
- consolidates AI reporter documentation into `backend/docs/ai_agents/README.md`
- merges B-016 into canonical `BACKLOG.md`
- deletes temporary patch/handoff Markdown files from repository root

No reporter profile files are removed or rewritten by this patch.
