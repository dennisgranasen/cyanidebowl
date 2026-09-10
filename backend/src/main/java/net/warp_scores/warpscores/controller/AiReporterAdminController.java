package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.domain.persistence.AiReporterRuntimeStateRepository;
import net.warp_scores.warpscores.model.AiReporterRuntimeState;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequestMapping("/admin/ai-reporters")
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class AiReporterAdminController {
    private final AiReporterRegistry registry;
    private final AiReporterEffectiveProfileService effectiveProfiles;
    private final AiReporterRuntimeStateRepository runtimeRepository;

    @GetMapping
    public List<AdminReporter> list() {
        return registry.all().stream()
                .sorted((a, b) -> a.getAlias().compareToIgnoreCase(b.getAlias()))
                .map(this::toAdminReporter)
                .toList();
    }

    @GetMapping("/{id}")
    public AdminReporter get(@PathVariable String id) {
        return toAdminReporter(registry.require(id));
    }

    @PutMapping("/{id}/runtime")
    public AdminReporter update(
            @PathVariable String id,
            @RequestBody RuntimeUpdate update) {

        var definition = registry.require(id);
        var runtime = runtimeRepository.findById(id).orElseGet(() -> {
            var state = new AiReporterRuntimeState();
            state.setReporterId(id);
            return state;
        });

        runtime.setEnabledOverride(update.enabledOverride());
        runtime.setReportsEnabledOverride(update.reportsEnabledOverride());
        runtime.setInteractionsEnabledOverride(update.interactionsEnabledOverride());
        runtime.setPlayerRatingsEnabledOverride(update.playerRatingsEnabledOverride());
        runtime.setWritingWeightOverride(update.writingWeightOverride());
        runtime.setCommentProbabilityOverride(update.commentProbabilityOverride());
        runtime.setReactionProbabilityOverride(update.reactionProbabilityOverride());
        runtime.setReplyProbabilityOverride(update.replyProbabilityOverride());
        runtime.setUpdatedAt(Instant.now());

        runtimeRepository.save(runtime);
        return toAdminReporter(definition);
    }

    private AdminReporter toAdminReporter(AiReporterDefinition definition) {
        var effective = effectiveProfiles.effective(definition);
        var runtime = runtimeRepository.findById(definition.getId()).orElse(null);

        return new AdminReporter(
                definition.getId(),
                definition.getAlias(),
                definition.getRace(),
                definition.getRole(),
                definition.getPortrait().getImage(),
                definition.getPortrait().getAvatar(),
                effective.enabled(),
                effective.reportsEnabled(),
                effective.interactionsEnabled(),
                effective.playerRatingsEnabled(),
                effective.writingWeight(),
                runtime);
    }

    public record RuntimeUpdate(
            Boolean enabledOverride,
            Boolean reportsEnabledOverride,
            Boolean interactionsEnabledOverride,
            Boolean playerRatingsEnabledOverride,
            Double writingWeightOverride,
            Double commentProbabilityOverride,
            Double reactionProbabilityOverride,
            Double replyProbabilityOverride) {}

    public record AdminReporter(
            String id,
            String alias,
            String race,
            String role,
            String portraitImage,
            String avatarImage,
            boolean enabled,
            boolean reportsEnabled,
            boolean interactionsEnabled,
            boolean playerRatingsEnabled,
            double writingWeight,
            AiReporterRuntimeState runtime) {}
}
