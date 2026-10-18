# Pending AI work overview

The site-admin endpoint `/admin/ai-autonomous-work/pending` projects existing
work without enqueueing or executing anything. The autonomous-work page refreshes
this projection and the existing queue overview every ten seconds. Refresh errors
leave the previous snapshot visible and mark it as potentially stale.

Included sources:

- Dedicated Fan reconciliation jobs, with live per-team desired/active counts,
  new profiles remaining, reactivations and deactivations.
- Persisted autonomous articles, match articles, comments and replies.
- Persisted player-rating tasks per reporter.
- In-memory fan player-rating jobs, explicitly identified as memory-only.
- Persisted avatar and profile-image requests, labelled with fan and team.
- Replay analysis backlog and processing records, with missing originals marked
  blocked. Replay parsing is distinguished from LLM generation.

Service routing is resolved for jobs whose routing identity/task is known. Other
jobs state that their handler or individual fan resolves routing. Execution queues
also retain running calls in their job lists. Content jobs and their execution
calls describe different levels of the same work and must not be summed.

Editor-requested AI articles are stored as durable jobs before generation starts.
The request returns HTTP 202 with a job id, and the editor polls that job until
it completes or fails; a browser timeout no longer cancels the generation request.
These jobs use
priority 100 by default and retain failures as inactive records until an
administrator removes them. The original editor identity and permissions are
rechecked when the job runs.

Editorial image requests are also persisted before provider execution. Their
priority controls the order of approved, developing requests. Failed image jobs
remain visible and inactive until removed. The AI jobs management page shows both
editorial queues, along with the provider execution queues, with independent
10-job pages and fixed-height table viewports. Each row includes its current
state, estimated start where applicable, and failure details. Start estimates
are operational estimates (about one minute per queued job, plus the image
worker's polling delay), not guaranteed execution times; provider cooldowns and
ongoing calls can move them.

The lower-level text-provider execution queues remain in memory; failed provider
calls remain visible there until removed but are not retried automatically.
Remaining fan counts represent current required work, not an immutable original
batch size, and decrease as reconciliation saves each new profile. Unknown team
fan counts are displayed as unknown, rather than zero.

The endpoint is guarded by the same site-admin authorization as the queue controls.
It exposes task summaries rather than generation prompts. The initial projection
scans the queue collections and profile metadata; large deployments may require
pagination and indexed aggregation instead of returning the complete backlog.
