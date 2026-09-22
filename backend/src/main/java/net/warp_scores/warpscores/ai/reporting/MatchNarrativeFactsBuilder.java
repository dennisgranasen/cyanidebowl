package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.model.ReplayAnalysis;

/**
 * Anti-corruption layer between mutable replay-analysis details and stable LLM facts.
 */
public interface MatchNarrativeFactsBuilder {
    MatchNarrativeFacts build(ReplayAnalysis analysis);
}
