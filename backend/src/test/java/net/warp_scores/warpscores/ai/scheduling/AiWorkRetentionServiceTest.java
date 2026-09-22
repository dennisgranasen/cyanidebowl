package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiWorkRetentionServiceTest {
    @Test
    void cleanupRemovesCompletedWorkSoonerThanFailures() {
        AiAutonomousWorkQueue autonomousWork = mock(AiAutonomousWorkQueue.class);
        AiCommunityMediaGenerationRequestRepository mediaRequests =
                mock(AiCommunityMediaGenerationRequestRepository.class);
        AiWorkRetentionService service = new AiWorkRetentionService(autonomousWork, mediaRequests);

        service.cleanup();

        verify(autonomousWork).deleteItemsByStatusCompletedBefore(
                eq(AiAutonomousWorkItem.Status.SUCCEEDED), any(Instant.class));
        verify(autonomousWork).deleteItemsByStatusCompletedBefore(
                eq(AiAutonomousWorkItem.Status.FAILED), any(Instant.class));
        verify(mediaRequests).deleteByStatusInAndCompletedAtBefore(
                eq(List.of(
                        AiCommunityMediaGenerationRequest.Status.COMPLETED,
                        AiCommunityMediaGenerationRequest.Status.REJECTED)),
                any(Instant.class));
        verify(mediaRequests).deleteByStatusInAndCompletedAtBefore(
                eq(List.of(AiCommunityMediaGenerationRequest.Status.FAILED)),
                any(Instant.class));
    }
}