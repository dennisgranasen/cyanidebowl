package net.warp_scores.warpscores.identity;

/**
 * Base interface for entities with a MongoDB id.
 */
public interface Identity {
    String getId();
    int getOpus();
}