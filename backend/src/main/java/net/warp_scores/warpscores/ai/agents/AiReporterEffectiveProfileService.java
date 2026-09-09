package net.warp_scores.warpscores.ai.agents;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiReporterRuntimeStateRepository;
import net.warp_scores.warpscores.model.AiReporterRuntimeState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReporterEffectiveProfileService {
    private final AiReporterRegistry registry;
    private final AiReporterRuntimeStateRepository runtimeRepository;

    public EffectiveReporter effective(AiReporterDefinition definition) {
        AiReporterRuntimeState runtime = runtimeRepository.findById(definition.getId()).orElse(null);

        boolean enabled = runtime != null && runtime.getEnabledOverride() != null
                ? runtime.getEnabledOverride() : definition.isEnabled();

        boolean reports = enabled && value(
                runtime == null ? null : runtime.getReportsEnabledOverride(),
                definition.getCapabilities().isReports());
        boolean interactions = enabled && value(
                runtime == null ? null : runtime.getInteractionsEnabledOverride(),
                definition.getCapabilities().isInteractions());
        boolean ratings = enabled && value(
                runtime == null ? null : runtime.getPlayerRatingsEnabledOverride(),
                definition.getCapabilities().isPlayerRatings())
                && definition.getRating().isEnabled();

        double writingWeight = runtime != null && runtime.getWritingWeightOverride() != null
                ? runtime.getWritingWeightOverride() : definition.getBehaviour().getWritingWeight();

        return new EffectiveReporter(definition, enabled, reports, interactions, ratings, writingWeight);
    }

    public List<EffectiveReporter> enabledForReports() {
        return registry.all().stream().map(this::effective).filter(EffectiveReporter::reportsEnabled).toList();
    }

    public List<EffectiveReporter> enabledForRatings() {
        return registry.all().stream().map(this::effective).filter(EffectiveReporter::playerRatingsEnabled).toList();
    }

    public List<EffectiveReporter> enabledForInteractions() {
        return registry.all().stream().map(this::effective).filter(EffectiveReporter::interactionsEnabled).toList();
    }

    private static boolean value(Boolean override, boolean inherited) {
        return override != null ? override : inherited;
    }

    public record EffectiveReporter(
            AiReporterDefinition definition,
            boolean enabled,
            boolean reportsEnabled,
            boolean interactionsEnabled,
            boolean playerRatingsEnabled,
            double writingWeight) {}
}
