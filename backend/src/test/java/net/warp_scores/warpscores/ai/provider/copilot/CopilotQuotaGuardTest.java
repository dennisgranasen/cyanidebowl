package net.warp_scores.warpscores.ai.provider.copilot;

import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.ai.provider.AiProviderProperties;
import net.warp_scores.warpscores.model.AiSettings;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CopilotQuotaGuardTest {
    @Test
    void usesEnvironmentDefaultAndRequiresConfirmationAtOrBelowThreshold() {
        var provider = mock(GitHubCopilotLlmProvider.class);
        when(provider.isConfigured()).thenReturn(true);
        when(provider.accountQuota()).thenReturn(new GitHubCopilotLlmProvider.CopilotAccountQuota(
                false, 300L, 240L, 20.0, 0.0, OffsetDateTime.parse("2026-11-01T00:00:00Z")));
        var settings = mock(AiSettingsRepository.class);
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.empty());
        var guard = new CopilotQuotaGuard(provider, settings, new AiProviderProperties(), 20);

        assertThat(guard.status().minimumRemainingPercent()).isEqualTo(20);
        assertThatThrownBy(() -> guard.requireConfirmation(false))
                .isInstanceOf(CopilotQuotaGuard.QuotaConfirmationRequiredException.class)
                .hasMessageContaining("20.0% remaining");

        guard.requireConfirmation(true);
    }

    @Test
    void aRuntimeThresholdOverridesTheEnvironmentDefault() {
        var provider = mock(GitHubCopilotLlmProvider.class);
        var settings = mock(AiSettingsRepository.class);
        var configured = new AiSettings();
        configured.setCopilotMinimumRemainingPercent(10);
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.of(configured));
        var guard = new CopilotQuotaGuard(provider, settings, new AiProviderProperties(), 20);

        assertThat(guard.minimumRemainingPercent()).isEqualTo(10);
    }
}
