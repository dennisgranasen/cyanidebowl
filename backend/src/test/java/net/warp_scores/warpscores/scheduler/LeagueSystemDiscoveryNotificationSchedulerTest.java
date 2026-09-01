package net.warp_scores.warpscores.scheduler;

import net.warp_scores.warpscores.controller.LeagueSystemDiscoveryCandidate;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.Platform;
import net.warp_scores.warpscores.service.LeagueSystemDiscoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeagueSystemDiscoveryNotificationSchedulerTest {
    private final LeagueSystemRepository systems = mock(LeagueSystemRepository.class);
    private final LeagueSystemDiscoveryService discovery = mock(LeagueSystemDiscoveryService.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final LeagueSystemDiscoveryNotificationScheduler scheduler =
            new LeagueSystemDiscoveryNotificationScheduler(systems, discovery, mail);

    @Test
    void sendsOnlyNewCandidatesAndPersistsTheirIds() {
        LeagueSystem system = enabledSystem();
        var oldCandidate = candidate("Competition:old");
        var newCandidate = candidate("Competition:new");
        system.setNotifiedDiscoveryCandidateIds(List.of(oldCandidate.candidateId()));
        when(systems.findAll()).thenReturn(List.of(system));
        when(discovery.discover(system)).thenReturn(List.of(oldCandidate, newCandidate));

        scheduler.notifyAboutNewCandidates();

        verify(mail).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(systems).save(system);
        assertThat(system.getNotifiedDiscoveryCandidateIds())
                .containsExactlyInAnyOrder("Competition:old", "Competition:new");
    }

    @Test
    void doesNothingWhenPerSystemNotificationsAreDisabled() {
        LeagueSystem system = enabledSystem();
        system.setDiscoveryNotificationEnabled(false);
        when(systems.findAll()).thenReturn(List.of(system));

        scheduler.notifyAboutNewCandidates();

        verify(discovery, never()).discover(any());
        verify(mail, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    private LeagueSystem enabledSystem() {
        LeagueSystem system = new LeagueSystem();
        system.setId("nst");
        system.setName("NST");
        system.setDiscoveryNotificationEnabled(true);
        system.setDiscoveryNotificationEmail("admin@example.test");
        return system;
    }

    private LeagueSystemDiscoveryCandidate candidate(String id) {
        return new LeagueSystemDiscoveryCandidate(id, id.substring("Competition:".length()),
                EntityType.Competition, "NST", "NST XXIV", 24,
                GameType.BB2, Platform.PC, new Date(), 12);
    }
}
