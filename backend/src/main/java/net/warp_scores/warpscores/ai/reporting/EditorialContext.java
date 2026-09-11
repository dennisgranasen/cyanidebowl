package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Context that may influence framing but must never override match facts.
 */
@Value
@Builder
public class EditorialContext {
    CompetitionContext competition;
    Map<String, TeamFormContext> teamForm;
    List<PlayerNarrativeSignal> playerSignals;
    List<ContextArticle> relatedArticles;
    RivalryContext rivalry;
    Map<String, Object> rosterAvailability;

    /**
     * Reserved extension area for future deterministic context builders.
     */
    @Singular("extension")
    Map<String, Object> extensions;
}
