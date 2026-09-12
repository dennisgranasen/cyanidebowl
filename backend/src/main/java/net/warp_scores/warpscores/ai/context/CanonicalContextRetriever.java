package net.warp_scores.warpscores.ai.context;

import java.util.Collection;
import java.util.List;

/** Deterministic database retrieval contract. Ranking/token budgeting belongs to AI-004. */
public interface CanonicalContextRetriever {
    List<ContextItem> currentThread(SubjectRef thread, int limit);

    List<ContextItem> selfHistory(long authorUserId, Collection<SubjectRef> subjects, int limit);

    List<ContextItem> discourse(long excludingUserId, Collection<SubjectRef> subjects, int limit);

    List<ContextItem> domainContext(SubjectRef root, int limit);
}
