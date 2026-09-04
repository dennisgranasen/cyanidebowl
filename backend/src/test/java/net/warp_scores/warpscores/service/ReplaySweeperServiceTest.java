package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.domain.persistence.ReplaySweeperConfigurationRepository;
import net.warp_scores.warpscores.domain.persistence.ReplaySweeperLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReplaySweeperServiceTest {
    @Test
    void concurrentRequestsAreRejectedInsteadOfQueued() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        ReplaySweeperService service = service(events);

        assertTrue(service.run());
        assertFalse(service.run());
        verify(events, times(1)).publishEvent(any(ReplaySweepRequestedEvent.class));
    }

    @Test
    void eventDispatchFailureReleasesExecutionSlot() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        doThrow(new IllegalStateException("executor unavailable"))
                .doNothing()
                .when(events).publishEvent(any(ReplaySweepRequestedEvent.class));
        ReplaySweeperService service = service(events);

        assertThrows(IllegalStateException.class, service::run);
        assertTrue(service.run());
    }

    private ReplaySweeperService service(ApplicationEventPublisher events) {
        return new ReplaySweeperService(
                mock(ReplaySweeperConfigurationRepository.class),
                mock(ReplayDownloadRepository.class),
                mock(MatchRepository.class),
                mock(ReplaySweeperLogRepository.class),
                mock(PyBb3Client.class),
                events,
                mock(ReplayArtifactService.class));
    }
}
