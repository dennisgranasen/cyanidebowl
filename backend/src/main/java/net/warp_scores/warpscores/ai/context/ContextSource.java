package net.warp_scores.warpscores.ai.context;

/** Retrieval family. This is orthogonal to whether the item is factual or authored discourse. */
public enum ContextSource {
    CURRENT_THREAD,
    SELF,
    OTHER_USERS,
    DOMAIN,
    MEMORY
}
