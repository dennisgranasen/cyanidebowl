package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "warpscores.ai-reporting",
        name = "enabled",
        havingValue = "true")
public class ReporterPlayerRatingService {
    private final AiReporterEffectiveProfileService effectiveProfiles;
    private final AiPlayerMatchRatingRepository ratings;
    private final PlayerRatingGenerator generator;

    public void rateMatch(PlayerRatingFacts facts) {
        for (var reporter : effectiveProfiles.enabledForRatings()) {
            boolean alreadyExists = !ratings.findByMatchIdAndReporterId(
                    facts.getMatchId(), reporter.definition().getId()).isEmpty();
            if (!alreadyExists) {
                generator.generateAndPersist(reporter.definition(), facts);
            }
        }
    }

    public interface PlayerRatingGenerator {
        void generateAndPersist(AiReporterDefinition reporter, PlayerRatingFacts facts);
    }
}
