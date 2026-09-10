# AI reporter UI fix v2

Fixes exactly these issues:

1. Admin table avatars
   - The current backend AdminReporter DTO does not expose an image at all.
   - This patch adds `portraitImage` and `avatarImage`.
   - The table uses `avatarImage || portraitImage`.

2. `/staff` gallery
   - Removes every admin Edit button.
   - Cards only open the public reporter profile.

3. `/staff/:reporterId`
   - Remove the Edit button.
   - Replace it with an admin-only gear.
   - Gear opens inline runtime settings for exactly that reporter.

4. Shared runtime controls
   - Inline profile settings use the same runtime fields as bulk admin.

Included complete replacements:
- frontend/src/pages/StaffPage.jsx
- frontend/src/pages/AdminAiReportersPage.jsx
- frontend/src/components/ai-reporters/AiReporterRuntimeControls.jsx
- backend/src/main/java/net/warp_scores/warpscores/controller/AiReporterAdminController.java

ReporterProfilePage is a focused edit because it was just redesigned and may have local changes.
See REPORTER_PROFILE_CHANGE.md.
