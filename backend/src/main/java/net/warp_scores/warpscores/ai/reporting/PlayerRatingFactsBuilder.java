package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.model.ReplayAnalysis;

/**
 * Builds deterministic per-player facts once per match. Every active rating agent receives
 * the same factual payload; only persona/bias differs.
 */
public interface PlayerRatingFactsBuilder {
    PlayerRatingFacts build(ReplayAnalysis analysis);
}
