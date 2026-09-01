package net.warp_scores.warpscores.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.controller.LeagueSystemDiscoveryCandidate;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.service.LeagueSystemDiscoveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "league-system.discovery.notifications.enabled",
        havingValue = "true")
public class LeagueSystemDiscoveryNotificationScheduler {
    private final LeagueSystemRepository leagueSystems;
    private final LeagueSystemDiscoveryService discoveryService;
    private final JavaMailSender mailSender;

    @Value("${league-system.discovery.notifications.from:}")
    private String from;

    @Scheduled(cron = "${league-system.discovery.notifications.cron:0 0 8 * * *}")
    public void notifyAboutNewCandidates() {
        leagueSystems.findAll().stream()
                .filter(system -> Boolean.TRUE.equals(system.getDiscoveryNotificationEnabled()))
                .filter(system -> system.getDiscoveryNotificationEmail() != null
                        && !system.getDiscoveryNotificationEmail().isBlank())
                .forEach(this::notifyLeagueSystem);
    }

    private void notifyLeagueSystem(LeagueSystem system) {
        Set<String> notified = new HashSet<>(system.getNotifiedDiscoveryCandidateIds() == null
                ? List.of() : system.getNotifiedDiscoveryCandidateIds());
        List<LeagueSystemDiscoveryCandidate> newCandidates = discoveryService.discover(system).stream()
                .filter(candidate -> !notified.contains(candidate.candidateId()))
                .toList();
        if (newCandidates.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) message.setFrom(from);
        message.setTo(system.getDiscoveryNotificationEmail());
        message.setSubject("New LeagueSystem source candidates for " + system.getName());
        message.setText(messageBody(system, newCandidates));
        mailSender.send(message);

        List<String> updated = new ArrayList<>(notified);
        updated.addAll(newCandidates.stream().map(LeagueSystemDiscoveryCandidate::candidateId).toList());
        system.setNotifiedDiscoveryCandidateIds(updated);
        leagueSystems.save(system);
        log.info("Sent LeagueSystem discovery notification for {} with {} candidate(s)",
                system.getId(), newCandidates.size());
    }

    private String messageBody(LeagueSystem system, List<LeagueSystemDiscoveryCandidate> candidates) {
        StringBuilder body = new StringBuilder("Potential new seasons or sources were found for ")
                .append(system.getName()).append(":\n\n");
        candidates.forEach(candidate -> body.append("- ")
                .append(candidate.competitionName() == null ? candidate.sourceEntityId() : candidate.competitionName())
                .append(" (suggested season ")
                .append(candidate.suggestedSeasonNumber() == null ? "unknown" : candidate.suggestedSeasonNumber())
                .append(", ").append(candidate.matchCount()).append(" matches)\n"));
        return body.append("\nReview and confirm them on the LeagueSystem admin page.").toString();
    }
}
