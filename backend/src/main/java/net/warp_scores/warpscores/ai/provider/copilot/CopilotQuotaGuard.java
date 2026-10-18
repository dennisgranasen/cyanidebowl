package net.warp_scores.warpscores.ai.provider.copilot;

import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.ai.provider.AiProviderProperties;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CopilotQuotaGuard {
    private final GitHubCopilotLlmProvider provider;
    private final AiSettingsRepository settingsRepository;
    private final AiProviderProperties providerProperties;
    private final int environmentDefault;

    public CopilotQuotaGuard(
            GitHubCopilotLlmProvider provider,
            AiSettingsRepository settingsRepository,
            AiProviderProperties providerProperties,
            @Value("${warpscores.ai.providers.copilot.minimum-remaining-percent:20}")
            int environmentDefault) {
        validatePercent(environmentDefault);
        this.provider = provider;
        this.settingsRepository = settingsRepository;
        this.providerProperties = providerProperties;
        this.environmentDefault = environmentDefault;
    }

    public List<ModelOverride> availableModelOverrides() {
        if (!provider.isConfigured()) return List.of();
        return providerProperties.getTargets().entrySet().stream()
                .filter(entry -> isCopilotTarget(entry.getKey()))
                .map(entry -> new ModelOverride(
                        entry.getKey(), "GitHub Copilot", entry.getValue().getModel()))
                .toList();
    }

    public boolean isCopilotTarget(String targetId) {
        AiProviderProperties.TargetConfig target = providerProperties.getTargets().get(targetId);
        return target != null
                && target.getModality() == AiProviderProperties.Modality.TEXT
                && "github-copilot".equals(target.getProvider());
    }

    public QuotaStatus status() {
        int threshold = minimumRemainingPercent();
        if (!provider.isConfigured()) {
            return new QuotaStatus(false, provider.configurationIssue(), threshold, null);
        }
        try {
            return new QuotaStatus(true, null, threshold, provider.accountQuota());
        } catch (RuntimeException failure) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not read GitHub Copilot quota: " + failure.getMessage(),
                    failure);
        }
    }

    public int setMinimumRemainingPercent(int percent) {
        validatePercent(percent);
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        settings.setCopilotMinimumRemainingPercent(percent);
        settingsRepository.save(settings);
        return percent;
    }

    public void requireConfirmation(boolean confirmed) {
        QuotaStatus status = status();
        if (!status.configured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub Copilot is not configured: " + status.message());
        }
        GitHubCopilotLlmProvider.CopilotAccountQuota quota = status.premiumInteractions();
        if (quota == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub Copilot did not return a premium-interaction quota; refusing to run without a budget check");
        }
        if (Boolean.TRUE.equals(quota.unlimited())) return;
        if (quota.remainingPercentage() == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub Copilot did not return remaining quota; refusing to run without a budget check");
        }

        if (quota.remainingPercentage() <= status.minimumRemainingPercent() && !confirmed) {
            long used = quota.usedRequests() == null ? -1 : quota.usedRequests();
            long entitlement = quota.entitlementRequests() == null ? -1 : quota.entitlementRequests();
            String reset = quota.resetDate() == null ? "unknown" : quota.resetDate().toString();
            throw new QuotaConfirmationRequiredException(
                    "Copilot premium-interaction quota is at "
                            + quota.remainingPercentage() + "% remaining ("
                            + used + "/" + entitlement + " used; resets " + reset
                            + "). The configured confirmation threshold is "
                            + status.minimumRemainingPercent()
                            + "%. Continue with this explicitly requested Copilot job?");
        }
    }

    public int minimumRemainingPercent() {
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        Integer configured = settings.getCopilotMinimumRemainingPercent();
        int percent = configured == null ? environmentDefault : configured;
        validatePercent(percent);
        return percent;
    }

    private static void validatePercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Copilot minimum remaining percentage must be 0..100");
        }
    }

    public static class QuotaConfirmationRequiredException extends RuntimeException {
        public QuotaConfirmationRequiredException(String message) {
            super(message);
        }
    }

    public record QuotaStatus(
            boolean configured,
            String message,
            int minimumRemainingPercent,
            GitHubCopilotLlmProvider.CopilotAccountQuota premiumInteractions) {
    }

    public record ModelOverride(String targetId, String provider, String model) {
    }
}
