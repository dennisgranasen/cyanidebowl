package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.AiProviderWorkExecutor;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DedicatedFanPlayerRatingJobServiceTest {
    private final DedicatedFanPlayerRatingService generator = mock(DedicatedFanPlayerRatingService.class);
    private final CopilotRatingQuotaUsageService quotaUsage = mock(CopilotRatingQuotaUsageService.class);
    private final LlmProviderRouter providerRouter = mock(LlmProviderRouter.class);
    private final AiProviderWorkExecutor providerWorkExecutor = mock(AiProviderWorkExecutor.class);
    private final DedicatedFanPlayerRatingJobService service = new DedicatedFanPlayerRatingJobService(
            generator, quotaUsage, providerRouter, providerWorkExecutor);

    @Test
    void automaticQueueIncludesEveryUnratedActiveFanAndSkipsDuplicateActiveJob() {
        List<AiCommunityMemberProfile> fans = IntStream.range(0, 12)
                .mapToObj(index -> {
                    AiCommunityMemberProfile fan = mock(AiCommunityMemberProfile.class);
                    when(fan.getId()).thenReturn("fan-" + index);
                    when(fan.getDisplayName()).thenReturn("Fan " + index);
                    return fan;
                })
                .toList();
        PlayerRatingFacts facts = PlayerRatingFacts.builder()
                .matchId("match-1")
                .players(List.of(PlayerRatingFacts.Player.builder().playerId("player-1").build()))
                .build();
        when(generator.unratedActiveFansFor(facts)).thenReturn(fans);
        when(providerRouter.planForTask(anyString(), eq(ContextTaskType.PLAYER_RATING), any()))
                .thenReturn(new LlmProviderRouter.ExecutionPlan(
                        new LlmProviderRouter.ModelTarget("provider", "model"), List.of(), 50));

        service.queueAfterReplayAnalysis("match-1", facts);
        service.queueAfterReplayAnalysis("match-1", facts);

        verify(generator, times(2)).unratedActiveFansFor(facts);
        verify(quotaUsage).start(anyString(), eq("match-1"),
                eq(CopilotRatingQuotaUsage.RatingKind.FANS), eq("replay-analysis"), isNull(), eq(12));
        verify(providerWorkExecutor, times(12)).submit(anyString(), any(), eq(50), any(Runnable.class));
        verifyNoMoreInteractions(quotaUsage);
    }

    @Test
    void automaticQueueDoesNothingWhenNoUnratedFansExist() {
        PlayerRatingFacts facts = PlayerRatingFacts.builder()
                .matchId("match-1")
                .players(List.of(PlayerRatingFacts.Player.builder().playerId("player-1").build()))
                .build();
        when(generator.unratedActiveFansFor(facts)).thenReturn(List.of());

        service.queueAfterReplayAnalysis("match-1", facts);

        verify(generator).unratedActiveFansFor(facts);
        verifyNoInteractions(quotaUsage, providerRouter, providerWorkExecutor);
    }
}