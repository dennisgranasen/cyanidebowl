package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.RegisteredSourceRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.RegisteredSource;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DedicatedFanReconciliationQueueService {
    private final TeamRepository teams;
    private final DedicatedFanReconciliationJobRepository jobs;
    private final AiCommunityMemberProfileRepository profiles;
    private final LeagueSystemRepository leagueSystems;
    private final SeasonRepository seasons;
    private final RegisteredSourceRepository registeredSources;
    private final StageSourceRepository stageSources;
    private final LlmProviderRouter providerRouter;
    private final DedicatedFanProviderCooldowns providerCooldowns;

    public QueueSummary enqueueAll() {
        return enqueueAll(null);
    }

    public QueueSummary enqueueAll(String targetOverride) {
        int scanned = 0;
        int queued = 0;
        int unchanged = 0;
        int unknownFanCounts = 0;
        var unknownFanCountTeamIds = new java.util.ArrayList<String>();
        Instant now = Instant.now();
        var allTeams = teams.findAll().stream()
                .filter(team -> team != null && team.getId() != null)
                .filter(team -> !Boolean.TRUE.equals(team.getIsDeleted()))
                .toList();
        Map<String, Integer> activeProfilesByTeam = new HashMap<>();
        for (var profile : profiles.findByTeamIdIn(
                allTeams.stream().map(team -> team.getId().asMongoKey()).toList())) {
            if (profile.isActive()) {
                String profileTeamId = profile.getTeamId();
                activeProfilesByTeam.put(
                        profileTeamId,
                        activeProfilesByTeam.getOrDefault(profileTeamId, 0) + 1);
            }
        }
        Map<String, DedicatedFanReconciliationJob> existingJobs = new HashMap<>();
        for (var job : jobs.findAll()) existingJobs.put(job.getTeamId(), job);
        Map<SourceKey, Integer> mainLeagueSources = mainLeagueSources();

        for (var team : allTeams) {
            scanned++;
            String teamId = team.getId().asMongoKey();
            DedicatedFanReconciliationJob existing = existingJobs.get(teamId);
            if (team.getDedicatedFans() == null) {
                unknownFanCounts++;
                unknownFanCountTeamIds.add(teamId);
                removeObsoleteQueuedJob(existing);
                continue;
            }
            int desired = Math.max(0, team.getDedicatedFans());
            int active = activeProfilesByTeam.getOrDefault(teamId, 0);
            if (active == desired) {
                unchanged++;
                removeObsoleteQueuedJob(existing);
                continue;
            }
            if (existing != null && existing.getStatus() == DedicatedFanReconciliationJob.Status.RUNNING) {
                continue;
            }
            DedicatedFanReconciliationJob job = existing;
            if (job == null) job = new DedicatedFanReconciliationJob();
            if (targetOverride != null && !targetOverride.isBlank()) {
                job.setTargetOverride(targetOverride);
            }
            Instant providerCooldown = null;
            String effectiveTarget = job.getTargetOverride();
            if (effectiveTarget != null && !effectiveTarget.isBlank()) {
                String providerId = providerRouter.planForTask(
                    DedicatedFanAiProfileGenerator.AGENT_ID,
                    ContextTaskType.FAN_PROFILE,
                    new LlmProviderRouter.ExecutionOverrides(effectiveTarget, null))
                    .primary().providerId();
                providerCooldown = providerCooldowns.until(providerId);
            }
            job.setTeamId(teamId);
            job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
            job.setPriority(priorityFor(team, mainLeagueSources));
            job.setRequestedAt(providerCooldown != null && providerCooldown.isAfter(now)
                    ? providerCooldown
                    : now);
            job.setStartedAt(null);
            job.setLastError(null);
            jobs.save(job);
            queued++;
        }
        return new QueueSummary(
            scanned, queued, unchanged, unknownFanCounts, List.copyOf(unknownFanCountTeamIds), now);
    }

    private Map<SourceKey, Integer> mainLeagueSources() {
        LeagueSystem primary = leagueSystems.findAll().stream()
                .filter(system -> Boolean.TRUE.equals(system.getPrimary()))
                .findFirst()
                .orElse(null);
        if (primary == null || primary.getId() == null) return Map.of();

        Map<String, Integer> seasonNumbers = new HashMap<>();
        for (Season season : seasons.findByLeagueSystemIdOrderBySequenceAsc(primary.getId())) {
            if (season.getId() != null && season.getNumber() != null) {
                seasonNumbers.put(season.getId(), season.getNumber());
            }
        }

        Map<SourceKey, Integer> sources = new HashMap<>();
        for (RegisteredSource source : registeredSources.findByLeagueSystemId(primary.getId())) {
            addSource(sources, source.getSourceType(), source.getSourceEntityId(),
                    seasonNumbers.get(source.getSeasonId()));
        }
        for (StageSource source : stageSources.findByLeagueSystemId(primary.getId())) {
            addSource(sources, source.getSourceType(), source.getSourceEntityId(),
                    seasonNumbers.get(source.getSeasonId()));
        }
        return sources;
    }

    private static void addSource(
            Map<SourceKey, Integer> sources,
            EntityType type,
            Identity sourceId,
            Integer seasonNumber) {
        if (type == null || sourceId == null) return;
        SourceKey key = new SourceKey(type, sourceId.asMongoKey());
        int number = seasonNumber == null ? 0 : Math.max(0, seasonNumber);
        sources.put(key, Math.max(sources.getOrDefault(key, 0), number));
    }

    private static int priorityFor(Team team, Map<SourceKey, Integer> mainLeagueSources) {
        int seasonNumber = 0;
        boolean inMainLeagueSystem = false;
        Set<String> competitionIds = identityKeys(team.getCompetitionIds());
        Set<String> leagueIds = identityKeys(team.getLeagueIds());
        for (var source : mainLeagueSources.entrySet()) {
            boolean matches = source.getKey().type() == EntityType.Competition
                    ? competitionIds.contains(source.getKey().id())
                    : leagueIds.contains(source.getKey().id());
            if (matches) {
                inMainLeagueSystem = true;
                seasonNumber = Math.max(seasonNumber, source.getValue());
            }
        }
        return 1 + (inMainLeagueSystem ? 10 + seasonNumber : 0);
    }

    private static Set<String> identityKeys(Identity[] identities) {
        if (identities == null) return Set.of();
        Set<String> keys = new HashSet<>();
        for (Identity identity : identities) {
            if (identity != null) keys.add(identity.asMongoKey());
        }
        return keys;
    }

    private record SourceKey(EntityType type, String id) {}

    private void removeObsoleteQueuedJob(DedicatedFanReconciliationJob job) {
        if (job != null && job.getStatus() == DedicatedFanReconciliationJob.Status.QUEUED) {
            jobs.delete(job);
        }
    }

    public boolean reprioritize(String teamId, int priority) {
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority must be 0..100");
        }
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) {
            return false;
        }
        job.setPriority(priority);
        jobs.save(job);
        return true;
    }

    public synchronized boolean moveTarget(String teamId, String targetOverride) {
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) {
            return false;
        }
        job.setTargetOverride(targetOverride);
        String providerId = providerRouter.planForTask(
            DedicatedFanAiProfileGenerator.AGENT_ID,
            ContextTaskType.FAN_PROFILE,
            new LlmProviderRouter.ExecutionOverrides(targetOverride, null))
            .primary().providerId();
        Instant now = Instant.now();
        Instant providerCooldown = providerCooldowns.until(providerId);
        job.setRequestedAt(providerCooldown != null && providerCooldown.isAfter(now)
            ? providerCooldown
            : now);
        job.setLastError(null);
        jobs.save(job);
        return true;
    }

    public synchronized DedicatedFanReconciliationJob claim(String teamId) {
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) return null;
        job.setStatus(DedicatedFanReconciliationJob.Status.RUNNING);
        job.setStartedAt(Instant.now());
        job.setAttempts(job.getAttempts() + 1);
        return jobs.save(job);
    }

    public boolean remove(String teamId) {
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) {
            return false;
        }
        jobs.delete(job);
        return true;
    }

    public record QueueSummary(
            int scannedTeams,
            int queuedTeams,
            int unchangedTeams,
            int unknownFanCountTeams,
            List<String> unknownFanCountTeamIds,
            Instant queuedAt) {}
}
