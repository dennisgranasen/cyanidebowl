# AI context persistence — AI-008

AI-008 completes the B-018 foundation for the two previously empty canonical context
families: `social` and `memory`.

## Social relationships

`AiSocialRelationship` stores current deterministic relationships independently from
canonical user identity. Supported foundation relationship types are:

- `TEAM_AFFINITY`
- `COACH_IDENTITY`
- `LEAGUE_MEMBERSHIP`
- `AFFILIATION`

Relationships have stable IDs, active/inactive lifecycle, timestamps and subject
metadata. `AiSocialRelationshipStore.upsert(...)` is idempotent.

B-019 Dedicated Fans should use `TEAM_AFFINITY` and activation/deactivation rather than
adding supporter fields to `WarpScoresUser`.

Social relationship context is rendered as `DOMAIN_FACT` because the relationship
record itself is canonical application state. It is placed in the semantic `SOCIAL`
section by `ContextAssemblyService`; `ContextSource.DOMAIN` continues to describe its
authoritative storage origin.

## Episodic memory

`AiMemoryEntry` stores durable user-owned memory with:

- canonical owner user id;
- one or more `SubjectRef`s;
- compact memory text;
- source content references;
- active/inactive lifecycle;
- timestamps.

Memory is always rendered as `ATTRIBUTED_DISCOURSE`. Persisting or summarizing a claim
must never silently promote it to an objective fact.

AI-008 provides the persistence/retrieval seam only. Policy for deciding what deserves
to become memory, summarization frequency and automatic memory writes belong to later
agent-runtime work.

## Subject expansion

For a generation request, domain context is resolved first. Its subjects are merged
with the planned subjects before `social`, `self`, `discourse` and `memory` retrieval.

A match can therefore bring TEAM, COACH_IDENTITY, COMPETITION and LEAGUE_SYSTEM subjects
into the other context families automatically.

## Domain projection

Canonical match context now contains deterministic in-world sporting prose such as the
competition, round, result, coaches and venue. Mechanical implementation fields are
not projected into AI-facing domain text.

The hard world-model constraints in `WorldModelPolicy` remain the final platform-level
guardrail.
