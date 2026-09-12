package net.warp_scores.warpscores.ai.context;

/**
 * How a consumer is allowed to interpret a context item.
 * Authored text is testimony/opinion even when the author is an editor, coach or AI reporter.
 */
public enum ContextAuthority {
    ATTRIBUTED_DISCOURSE,
    DOMAIN_FACT
}
