### B-016 — AI reporter editorial scheduling and cost control

**Status: Backlog — intentionally deferred**

Implement after the canonical reporter registry/profile schema and first end-to-end AI editorial flow are stable.

- recurring AI editorial scheduling;
- per-reporter/global generation quotas;
- provider/token/cost budgets;
- queueing/backpressure and idempotent retries;
- cooldown enforcement for articles/comments/replies/reactions;
- priority handling for direct tags;
- admin visibility into queued/recent generation work;
- global, reporter and capability kill switches.

**Product rule:** a directly tagged reporter is always scheduled for a textual response when enabled and capable. Tagged work bypasses probabilistic selection; hard admin/global disables still win.
