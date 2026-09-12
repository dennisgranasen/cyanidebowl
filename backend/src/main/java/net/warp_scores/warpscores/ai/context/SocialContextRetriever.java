package net.warp_scores.warpscores.ai.context;

import java.util.Collection;
import java.util.List;

/** Retrieval boundary for deterministic current social relationships. */
public interface SocialContextRetriever {
    List<ContextItem> socialContext(long authorUserId, Collection<SubjectRef> subjects, int limit);
}
