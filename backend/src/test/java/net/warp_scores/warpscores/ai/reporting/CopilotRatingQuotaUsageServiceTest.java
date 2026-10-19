package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import net.warp_scores.warpscores.ai.provider.copilot.GitHubCopilotLlmProvider;
import net.warp_scores.warpscores.domain.persistence.CopilotRatingQuotaUsageRepository;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CopilotRatingQuotaUsageServiceTest {
    private final CopilotRatingQuotaUsageRepository repository =
            mock(CopilotRatingQuotaUsageRepository.class);
    private final CopilotQuotaGuard quotaGuard = mock(CopilotQuotaGuard.class);
    private final CopilotRatingQuotaUsageService service =
            new CopilotRatingQuotaUsageService(repository, quotaGuard);

    @Test
    void recordsQuotaBeforeAndAfterAndComputesObservedAccountDelta() {
        when(quotaGuard.availableModelOverrides()).thenReturn(List.of(
                new CopilotQuotaGuard.ModelOverride("copilot-gpt6", "GitHub Copilot", "GPT-6 Luna")));
        when(quotaGuard.status()).thenReturn(status(602), status(617));
        AtomicReference<CopilotRatingQuotaUsage> stored = new AtomicReference<>();
        when(repository.findById("job-1")).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repository.save(any(CopilotRatingQuotaUsage.class)))
                .thenAnswer(invocation -> {
                    CopilotRatingQuotaUsage value = invocation.getArgument(0);
                    stored.set(value);
                    return value;
                });

        service.start("job-1", "match-1", CopilotRatingQuotaUsage.RatingKind.EDITORIAL,
                "editor", "copilot-gpt6", 5);
        service.complete("job-1", 14, 1, 0, 0);

        var saved = org.mockito.ArgumentCaptor.forClass(CopilotRatingQuotaUsage.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        CopilotRatingQuotaUsage completed = saved.getAllValues().get(1);
        assertThat(completed.getModel()).isEqualTo("GPT-6 Luna");
        assertThat(completed.getQuotaBefore().usedRequests()).isEqualTo(602);
        assertThat(completed.getQuotaAfter().usedRequests()).isEqualTo(617);
        assertThat(completed.getObservedUsedRequestDelta()).isEqualTo(15);
        assertThat(completed.getSucceededCalls()).isEqualTo(14);
        assertThat(completed.getFailedCalls()).isEqualTo(1);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    void doesNotOverwriteAnAlreadyCompletedMeasurement() {
        CopilotRatingQuotaUsage completed = newUsage("job-2");
        completed.setCompletedAt(java.time.Instant.now());
        when(repository.findById("job-2")).thenReturn(Optional.of(completed));

        service.complete("job-2", 1, 0, 0, 0);

        verify(quotaGuard, never()).status();
        verify(repository, never()).save(any());
    }

        @Test
        void doesNotRecordJobsWithoutAnAvailableCopilotTarget() {
                when(quotaGuard.availableModelOverrides()).thenReturn(List.of());

                service.start("job-non-copilot", "match-1", CopilotRatingQuotaUsage.RatingKind.EDITORIAL,
                                "editor", "other-provider-model", 5);

                verify(quotaGuard, never()).status();
                verify(repository, never()).save(any());
        }

    private static CopilotQuotaGuard.QuotaStatus status(long used) {
        return new CopilotQuotaGuard.QuotaStatus(true, null, 20,
                new GitHubCopilotLlmProvider.CopilotAccountQuota(false, 7000L, used,
                        91.4, 0.0, OffsetDateTime.parse("2026-11-01T00:00:00Z")));
    }

    private static CopilotRatingQuotaUsage newUsage(String id) {
        CopilotRatingQuotaUsage usage = new CopilotRatingQuotaUsage();
        usage.setId(id);
        usage.setQuotaBefore(status(600).premiumInteractions());
        return usage;
    }
}
