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

The projection does not turn direct HTTP article/image requests into durable jobs.
Direct text calls are visible in the existing service execution queues while
waiting/running. Direct image-provider calls do not have persisted queue records.
Remaining fan counts represent current required work, not an immutable original
batch size, and decrease as reconciliation saves each new profile. Unknown team
fan counts are displayed as unknown, rather than zero.

The endpoint is guarded by the same site-admin authorization as the queue controls.
It exposes task summaries rather than generation prompts. The initial projection
scans the queue collections and profile metadata; large deployments may require
pagination and indexed aggregation instead of returning the complete backlog.
