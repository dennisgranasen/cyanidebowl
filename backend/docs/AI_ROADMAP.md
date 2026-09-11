# AI implementation roadmap

> Working branch: `dev`  
> Scope: AI identity, context, community population and editorial generation  
> Status: canonical design roadmap  
> This change is documentation-only. Implementation follows in a later step.

## Purpose

This roadmap defines the implementation order for the AI/editorial system.

The ordering is deliberate: identity, context semantics and community lifecycle must be
stable before provider integration, scheduling, quotas or cost control are added.

The existing `BACKLOG.md` remains authoritative for the general project backlog:

- B-014 remains the editorial/community layer.
- B-015 remains team comment streams.
- B-016 remains AI reporter editorial scheduling and cost control.

The AI foundation is documented as B-017 through B-020. Those items are prerequisites
for B-016 even though their backlog numbers are higher. Do not renumber B-014–B-016,
because those IDs already have established meanings.

## Non-negotiable decisions

1. There is one canonical `User` concept. `User.accountType` is `HUMAN | AI`.
   Older `USER | AI_AGENT` terminology is obsolete.
2. Roles/capabilities are independent from contextual relationships.
3. Coach is not a separate user type. A coach is a user who currently has a relevant
   game-aware `coachClaim`.
4. Generation provenance is independent from user identity.
5. Every AI invocation uses the canonical context model:
   `thread`, `social`, `self`, `discourse`, `memory`, `domain`.
6. Blood Bowl is a real sport in-universe. Internal simulation mechanics must never
   leak into AI-facing context or generated prose.
7. Dedicated Fans-backed community identities persist across activation/deactivation.
   Reconciliation never deletes them merely because the desired population decreases.

---

## Phase 0 — Freeze terminology and contracts

**Goal:** stop future implementation from inventing competing AI concepts.

Deliverables:

- Adopt `HUMAN | AI` as the only account-type terminology.
- Adopt the separation between identity, capability, contextual relationship and
  generation provenance.
- Adopt the context taxonomy and world-model rules from `AI_ARCHITECTURE.md`.
- Treat the architecture document as the contract for later provider/prompt work.

Exit criteria:

- “Who is this?”, “what may this user do?”, “what relationship does this user have to
  the subject?”, and “how was this content generated?” have separate answers.
- No implementation task needs to invent a second AI identity model or ad-hoc context
  categories.

## Phase 1 / B-017 — Canonical User and AI identity foundation

**Goal:** make AI participants first-class users without creating a parallel social model.

Implementation scope:

- Extend the canonical user domain with `accountType = HUMAN | AI`.
- Preserve existing Auth0-subject and game-aware `coachClaims` behavior for humans.
- Keep AI profile/persona data associated with the canonical user rather than creating
  a replacement identity hierarchy.
- Keep roles/capabilities separate from coach claims, team affinity, community
  membership and other contextual relationships.
- Add explicit generation provenance for generated content.

Acceptance criteria:

- Human and AI authors use the same editorial/community primitives.
- Existing human authorization and `coachClaims` remain intact.
- An AI user can have a role/capability without that implying team affinity.
- A human user can gain or lose a coach relationship without changing user type.
- Generated content identifies both the canonical author and generation provenance.

## Phase 2 / B-018 — Canonical context assembly and world model

**Goal:** every AI invocation receives deterministic, inspectable context instead of a
feature-specific prompt assembled ad hoc.

Implementation scope:

- Define one context envelope with six canonical sections:
  `thread`, `social`, `self`, `discourse`, `memory`, `domain`.
- Resolve relationships at generation time.
- Treat messages from humans and AI as statements in the same conversation/community
  model; authorship comes from the canonical user.
- Centralize the Blood Bowl world-model instructions and prohibited meta-language.
- Make context assembly testable without calling an LLM provider.

Acceptance criteria:

- The same context schema can serve comments, replies, direct tags and articles.
- Context assembly can explain why each included item is relevant.
- Coach relevance is derived from current claims/subject matter, not a permanent
  “coach user” classification.
- Tests prove that dice/RNG/game/replay/simulation language is not exposed as
  in-universe knowledge.

## Phase 3 / B-019 — Dedicated Fans community population

**Goal:** map team Dedicated Fans to a stable population of AI-backed
`COMMUNITY_MEMBER` users.

Implementation scope:

- Add a pure/testable `DedicatedFansPopulationReconciler`.
- Desired active population is the team Dedicated Fans value, currently 1–6.
- On first discovery, create clear team-affine community personas.
- On increase, reactivate inactive matching identities before creating new ones.
- On decrease, deactivate surplus identities without deleting them.
- Preserve posts, reactions, memories, relationships and provenance across lifecycle
  changes.
- Keep population reconciliation separate from content-generation scheduling.

Acceptance criteria:

- Reconciliation supports `create`, `reactivate`, `deactivate`, `unchanged`.
- Re-running reconciliation with unchanged input is idempotent.
- Reactivation preserves the same canonical user identity and history.
- No reconciliation path hard-deletes an identity because Dedicated Fans decreased.
- Unit tests require no provider, scheduler or network access.

## Phase 4 / B-020 — First end-to-end AI editorial flow

**Goal:** prove the architecture with one narrow textual interaction.

Recommended first vertical slice:

- one enabled AI user;
- one supported community target/thread;
- one explicit direct tag or otherwise deterministic trigger;
- canonical context assembly;
- provider call through a narrow provider abstraction;
- persisted generation provenance;
- persisted textual response through the existing editorial/community model.

Do not add broad autonomous posting, probabilistic activity or complex scheduling here.

Acceptance criteria:

- The flow uses the canonical `User`, context and provenance models.
- World-model policy is applied centrally.
- Provider failures do not create duplicate persisted responses on retry.
- The implementation is narrow enough to test end to end before scheduling exists.

## Phase 5 / B-016 — Scheduling, quotas and cost control

Only after B-017 through B-020 are stable should existing backlog item B-016 be
implemented.

B-016 owns operational policy:

- editorial scheduling and queue/backpressure;
- per-AI-user/global generation quotas;
- provider/token/cost budgets;
- cooldowns, retries and idempotency policy;
- admin visibility and kill switches;
- priority handling for direct tags.

A directly tagged enabled/capable AI user remains a deterministic textual trigger.
Scheduling policy may prioritize it, but hard global/admin disables still win.

**Hard rule:** do not use B-016 infrastructure to compensate for an unstable identity
model, ad-hoc context assembly, missing provenance or an undefined community lifecycle.

---

## Next coding session

Implement in this order:

1. Audit current `User`, permissions, `coachClaims`, comments/articles and author fields.
2. Implement the smallest migration-safe `HUMAN | AI` identity extension.
3. Add generation provenance without provider-specific coupling.
4. Add context DTOs/contracts and deterministic context assembly.
5. Add shared world-model policy and leakage tests.
6. Implement and unit-test Dedicated Fans reconciliation.
7. Build the first deterministic direct-tag generation flow.
8. Reassess B-016 only after B-017 through B-020 are green.

## Explicitly out of scope for the foundation

- high-volume autonomous social simulation;
- provider optimization/model routing;
- complex personality evolution;
- long-running probabilistic posting schedulers;
- token/cost dashboards beyond provenance needed by the first flow;
- AI-specific duplicates of existing editorial/community primitives.
