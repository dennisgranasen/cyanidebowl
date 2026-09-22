package net.warp_scores.warpscores.ai.context;

/**
 * Canonical semantic context sections.
 *
 * <p>These sections are provider-neutral. Provider adapters may render them differently,
 * but must not redefine their meaning.</p>
 */
public enum ContextSection {
    THREAD,
    SOCIAL,
    SELF,
    DISCOURSE,
    MEMORY,
    DOMAIN
}
