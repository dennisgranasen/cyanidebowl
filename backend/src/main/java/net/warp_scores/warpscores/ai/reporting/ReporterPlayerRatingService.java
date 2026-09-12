package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        rateMatch(facts, List.of(), null, false);
    }

    public List<String> rateMatch(
            PlayerRatingFacts facts,
            Collection<String> reporterIds,
            String instruction,
            boolean force) {
        Set<String> requested = reporterIds == null
                ? Set.of()
                : new LinkedHashSet<>(reporterIds);

        var enabled = effectiveProfiles.enabledForRatings();
        if (!requested.isEmpty()) {
            Set<String> enabledIds = enabled.stream()
                    .map(r -> r.definition().getId())
                    .collect(java.util.stream.Collectors.toSet());
            Set<String> unknown = requested.stream()
                    .filter(id -> !enabledIds.contains(id))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown or disabled rating reporters: " + unknown);
            }
        }

        var selected = enabled.stream()
                .filter(r -> requested.isEmpty()
                        || requested.contains(r.definition().getId()))
                .toList();

        for (var reporter : selected) {
            boolean alreadyExists = !ratings.findByMatchIdAndReporterId(
                    facts.getMatchId(), reporter.definition().getId()).isEmpty();
            if (force || !alreadyExists) {
                generator.generateAndPersist(
                        reporter.definition(), facts, instruction);
            }
        }

        return selected.stream()
                .map(r -> r.definition().getId())
                .toList();
    }

    public interface PlayerRatingGenerator {
        void generateAndPersist(
                AiReporterDefinition reporter,
                PlayerRatingFacts facts,
                String instruction);
    }
}
