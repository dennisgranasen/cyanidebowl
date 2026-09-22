package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.ai.interaction.EditorialImageRequestService;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.AiReporterRuntimeStateRepository;
import net.warp_scores.warpscores.model.AiSettings;
import net.warp_scores.warpscores.model.AiInitiativePolicy;
import net.warp_scores.warpscores.model.AiReporterRuntimeState;
import net.warp_scores.warpscores.model.EditorialImageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

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
    private final AiSettingsRepository settingsRepository;
    private final AiGenerationAdmissionService generationAdmission;
        private final EditorialImageRequestService imageRequests;

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

    @GetMapping("/settings")
    public AdminSettings settings() {
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        return new AdminSettings(normalizeLanguage(settings.getDefaultLanguage(), "sv"));
    }

    @PutMapping("/settings")
    public AdminSettings updateSettings(@RequestBody SettingsUpdate update) {
        if (update == null || !StringUtils.hasText(update.defaultLanguage())) {
            throw new IllegalArgumentException("defaultLanguage is required");
        }
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        settings.setDefaultLanguage(normalizeLanguage(update.defaultLanguage(), "sv"));
        settingsRepository.save(settings);
        return new AdminSettings(settings.getDefaultLanguage());
    }

    @GetMapping("/initiative-policy")
    public AiInitiativePolicy initiativePolicy() {
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        return settings.getInitiativePolicy() == null
                ? new AiInitiativePolicy()
                : settings.getInitiativePolicy();
    }

    @PutMapping("/initiative-policy")
    public AiInitiativePolicy updateInitiativePolicy(
            @RequestBody AiInitiativePolicy policy) {
        validateInitiativePolicy(policy);
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        settings.setInitiativePolicy(policy);
        settingsRepository.save(settings);
        return policy;
    }

        @GetMapping("/image-approval-policy")
        public EditorialImageRequest.ApprovalPolicy imageApprovalPolicy() {
                return settingsRepository.findById(AiSettings.GLOBAL_ID)
                                .map(AiSettings::getEditorialImageApprovalPolicy)
                                .orElse(EditorialImageRequest.ApprovalPolicy.EDITORIAL);
        }

        public record ImageApprovalPolicyUpdate(EditorialImageRequest.ApprovalPolicy policy) {}

        @PutMapping("/image-approval-policy")
        public EditorialImageRequest.ApprovalPolicy updateImageApprovalPolicy(
                        @RequestBody ImageApprovalPolicyUpdate update) {
                if (update == null || update.policy() == null) {
                        throw new IllegalArgumentException("image approval policy is required");
                }
                AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                                .orElseGet(AiSettings::new);
                settings.setEditorialImageApprovalPolicy(update.policy());
                settingsRepository.save(settings);
                return update.policy();
        }

        @GetMapping("/editorial-image-requests")
        public List<EditorialImageRequest> editorialImageRequests() {
                return imageRequests.list();
        }

        public record ReviewImageRequest(boolean approve) {}

        @PostMapping("/editorial-image-requests/{id}/review")
        public EditorialImageRequest reviewEditorialImageRequest(
                        @PathVariable String id,
                        @RequestBody ReviewImageRequest review,
                        java.security.Principal principal) {
                if (review == null) throw new IllegalArgumentException("review is required");
                return imageRequests.review(id, review.approve(), principal.getName(), true);
        }

    @GetMapping("/limits")
    public AdminGenerationLimits generationLimits() {
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        return toGenerationLimits(settings);
    }

    @PutMapping("/limits")
    public AdminGenerationLimits updateGenerationLimits(
            @RequestBody GenerationLimitsUpdate update) {
        if (update == null) {
            throw new IllegalArgumentException("generation limits payload is required");
        }

        validatePositive(
                "maxConcurrentGenerations",
                update.maxConcurrentGenerations());
        validatePositive(
                "maxSuccessfulGenerationsPerDay",
                update.maxSuccessfulGenerationsPerDay());
        validatePositive(
                "maxInputTokensPerDay",
                update.maxInputTokensPerDay());
        validatePositive(
                "maxOutputTokensPerDay",
                update.maxOutputTokensPerDay());

        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);

        settings.setGenerationEnabled(
                update.generationEnabled() == null
                        ? settings.isGenerationEffectivelyEnabled()
                        : update.generationEnabled());
        settings.setMaxConcurrentGenerations(update.maxConcurrentGenerations());
        settings.setMaxSuccessfulGenerationsPerDay(
                update.maxSuccessfulGenerationsPerDay());
        settings.setMaxInputTokensPerDay(update.maxInputTokensPerDay());
        settings.setMaxOutputTokensPerDay(update.maxOutputTokensPerDay());

        settingsRepository.save(settings);
        return toGenerationLimits(settings);
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
        runtime.setPrimaryLanguageOverride(normalizeLanguage(update.primaryLanguageOverride(), null));
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
                effective.primaryLanguage(),
                definition.getVoice().getPrimaryLanguage(),
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
            Double replyProbabilityOverride,
            String primaryLanguageOverride) {}

    public record SettingsUpdate(String defaultLanguage) {}
    public record AdminSettings(String defaultLanguage) {}

    public record GenerationLimitsUpdate(
            Boolean generationEnabled,
            Integer maxConcurrentGenerations,
            Integer maxSuccessfulGenerationsPerDay,
            Long maxInputTokensPerDay,
            Long maxOutputTokensPerDay) {}

    public record AdminGenerationLimits(
            boolean generationEnabled,
            Integer maxConcurrentGenerations,
            Integer maxSuccessfulGenerationsPerDay,
            Long maxInputTokensPerDay,
            Long maxOutputTokensPerDay,
            AiGenerationAdmissionService.UsageSnapshot usage) {}

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
            String primaryLanguage,
            String profileLanguage,
            AiReporterRuntimeState runtime) {}

    private AdminGenerationLimits toGenerationLimits(AiSettings settings) {
        return new AdminGenerationLimits(
                settings.isGenerationEffectivelyEnabled(),
                settings.getMaxConcurrentGenerations(),
                settings.getMaxSuccessfulGenerationsPerDay(),
                settings.getMaxInputTokensPerDay(),
                settings.getMaxOutputTokensPerDay(),
                generationAdmission.usageSnapshot());
    }

    private static void validateInitiativePolicy(AiInitiativePolicy policy) {
        if (policy == null || policy.getStaff() == null || policy.getFans() == null) {
            throw new IllegalArgumentException("staff and fans initiative policy are required");
        }
        if (policy.getStaff().getGeneralArticles() == null
                || policy.getStaff().getMatchArticles() == null
                || policy.getStaff().getArticleComments() == null
                || policy.getStaff().getMatchComments() == null
                || policy.getStaff().getDirectTagReplies() == null) {
            throw new IllegalArgumentException("all staff initiative modes are required");
        }

        validateProbability(
                "fans.generalArticleCommentProbability",
                policy.getFans().getGeneralArticleCommentProbability());
        validateProbability(
                "fans.ownTeamArticleCommentProbability",
                policy.getFans().getOwnTeamArticleCommentProbability());
        validateProbability(
                "fans.ownTeamMatchArticleCommentProbability",
                policy.getFans().getOwnTeamMatchArticleCommentProbability());
        validateProbability(
                "fans.ownTeamMatchCommentProbability",
                policy.getFans().getOwnTeamMatchCommentProbability());
        validateProbability(
                "fans.ownCoachActivityProbability",
                policy.getFans().getOwnCoachActivityProbability());
    }

    private static void validateProbability(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }

    private static void validatePositive(String field, Number value) {
        if (value != null && value.longValue() <= 0L) {
            throw new IllegalArgumentException(field + " must be positive or null");
        }
    }

    private static String normalizeLanguage(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        String normalized = value.trim().toLowerCase();
        if (!normalized.matches("[a-z]{2,3}([_-][a-z0-9]{2,8})?")) {
            throw new IllegalArgumentException("Invalid language code: " + value);
        }
        return normalized.replace('_', '-');
    }
}
