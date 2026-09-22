package net.warp_scores.warpscores.ai.context;

import java.util.Collection;
import java.util.List;

/** Retrieval boundary for durable, attributed AI memory. */
public interface MemoryContextRetriever {
    List<ContextItem> memoryContext(long ownerUserId, Collection<SubjectRef> subjects, int limit);
}
