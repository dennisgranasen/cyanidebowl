package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.RegisteredSourceRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.RegisteredSource;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DedicatedFanReconciliationQueueServiceTest {
    private final TeamRepository teams = mock(TeamRepository.class);
    private final DedicatedFanReconciliationJobRepository jobs =
            mock(DedicatedFanReconciliationJobRepository.class);
    private final AiCommunityMemberProfileRepository profiles =
            mock(AiCommunityMemberProfileRepository.class);
    private final LeagueSystemRepository leagueSystems = mock(LeagueSystemRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final RegisteredSourceRepository registeredSources = mock(RegisteredSourceRepository.class);
    private final StageSourceRepository stageSources = mock(StageSourceRepository.class);
    private final LlmProviderRouter providerRouter = mock(LlmProviderRouter.class);
    private final DedicatedFanProviderCooldowns providerCooldowns = new DedicatedFanProviderCooldowns();
    private final DedicatedFanReconciliationQueueService service = new DedicatedFanReconciliationQueueService(
            teams, jobs, profiles, leagueSystems, seasons, registeredSources, stageSources,
            providerRouter, providerCooldowns);

    @Test
    void queuesOnlyTeamsWhoseActiveFanCountDiffersAndReportsUnknownCounts() {
        Team unchanged = team(1, 2);
        Team needsFan = team(2, 2);
        Team unknown = team(3, null);
        Team deleted = team(4, 0);
        deleted.setIsDeleted(true);
        when(teams.findAll()).thenReturn(List.of(unchanged, needsFan, unknown, deleted));
        when(profiles.findByTeamIdIn(any())).thenReturn(List.of(
                profile(unchanged.getId().asMongoKey(), true),
                profile(unchanged.getId().asMongoKey(), true),
                profile(needsFan.getId().asMongoKey(), true)));
        when(jobs.findAll()).thenReturn(List.of());

        var summary = service.enqueueAll();

        assertThat(summary.scannedTeams()).isEqualTo(3);
        assertThat(summary.queuedTeams()).isEqualTo(1);
        assertThat(summary.unchangedTeams()).isEqualTo(1);
        assertThat(summary.unknownFanCountTeams()).isEqualTo(1);
        assertThat(summary.unknownFanCountTeamIds()).containsExactly(unknown.getId().asMongoKey());
        verify(jobs).save(org.mockito.ArgumentMatchers.argThat(job ->
            job.getTeamId().equals(needsFan.getId().asMongoKey())
                && job.getPriority() == 1));
        verify(jobs, org.mockito.Mockito.times(1)).save(any(DedicatedFanReconciliationJob.class));
    }

    @Test
    void removesOldQueuedJobWhenTeamNoLongerNeedsFanReconciliation() {
        Team team = team(5, 1);
        DedicatedFanReconciliationJob stale = new DedicatedFanReconciliationJob();
        stale.setTeamId(team.getId().asMongoKey());
        stale.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
        when(teams.findAll()).thenReturn(List.of(team));
        when(profiles.findByTeamIdIn(any())).thenReturn(List.of(profile(team.getId().asMongoKey(), true)));
        when(jobs.findAll()).thenReturn(List.of(stale));

        var summary = service.enqueueAll();

        assertThat(summary.queuedTeams()).isZero();
        assertThat(summary.unchangedTeams()).isEqualTo(1);
        verify(jobs).delete(stale);
        verify(jobs, org.mockito.Mockito.never()).save(any(DedicatedFanReconciliationJob.class));
    }

    @Test
    void movesQueuedJobAndPersistsTargetOverride() {
        DedicatedFanReconciliationJob job = new DedicatedFanReconciliationJob();
        job.setTeamId("team-1");
        job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
        job.setRequestedAt(Instant.now().plusSeconds(600));
        job.setLastError("Previous provider rate limit");
        when(jobs.findById("team-1")).thenReturn(Optional.of(job));
        when(providerRouter.planForTask(
            eq(DedicatedFanAiProfileGenerator.AGENT_ID),
            eq(ContextTaskType.FAN_PROFILE),
            any(LlmProviderRouter.ExecutionOverrides.class)))
            .thenReturn(new LlmProviderRouter.ExecutionPlan(
                new LlmProviderRouter.ModelTarget(
                    "copilot-gpt-6-luna", "copilot", "model", "copilot"),
                List.of(),
                50));
        providerCooldowns.extend("gemini", Instant.now().plusSeconds(900));
        Instant movedAt = Instant.now();

        assertTrue(service.moveTarget("team-1", "copilot-gpt-6-luna"));

        verify(jobs).save(job);
        org.junit.jupiter.api.Assertions.assertEquals("copilot-gpt-6-luna", job.getTargetOverride());
        assertThat(job.getRequestedAt()).isAfterOrEqualTo(movedAt);
        assertThat(job.getRequestedAt()).isBefore(Instant.now().plusSeconds(5));
        assertNull(job.getLastError());
    }

        @Test
        void appliesSelectedTargetToExistingQueuedJobsDuringReconciliation() {
        Team team = team(12, 2);
        DedicatedFanReconciliationJob existing = new DedicatedFanReconciliationJob();
        existing.setTeamId(team.getId().asMongoKey());
        existing.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
        existing.setTargetOverride("editorial");
        when(teams.findAll()).thenReturn(List.of(team));
        when(profiles.findByTeamIdIn(any())).thenReturn(List.of());
        when(jobs.findAll()).thenReturn(List.of(existing));
        when(leagueSystems.findAll()).thenReturn(List.of());
        when(providerRouter.planForTask(
            eq(DedicatedFanAiProfileGenerator.AGENT_ID),
            eq(ContextTaskType.FAN_PROFILE),
            any(LlmProviderRouter.ExecutionOverrides.class)))
            .thenReturn(new LlmProviderRouter.ExecutionPlan(
                new LlmProviderRouter.ModelTarget(
                    "copilot-gpt-6-luna", "copilot", "gpt-6-luna", "github-copilot"),
                List.of(), 50));

        var summary = service.enqueueAll("copilot-gpt-6-luna");

        assertThat(summary.queuedTeams()).isEqualTo(1);
        verify(jobs).save(org.mockito.ArgumentMatchers.argThat(job ->
            job == existing && "copilot-gpt-6-luna".equals(job.getTargetOverride())));
        }

    @Test
    void refusesToMoveRunningJob() {
        DedicatedFanReconciliationJob job = new DedicatedFanReconciliationJob();
        job.setTeamId("team-1");
        job.setStatus(DedicatedFanReconciliationJob.Status.RUNNING);
        when(jobs.findById("team-1")).thenReturn(Optional.of(job));

        assertFalse(service.moveTarget("team-1", "copilot-gpt-6-luna"));
    }

        @Test
        void appliesSelectedTargetAndMainLeagueSeasonPriorityToNewJobs() {
        Team team = team(6, 1);
            team.setCompetitionIds(new SimpleIdentity[]{new SimpleIdentity("cup-6", 3)});
        when(teams.findAll()).thenReturn(List.of(team));
        when(profiles.findByTeamIdIn(any())).thenReturn(List.of());
        when(jobs.findAll()).thenReturn(List.of());
            LeagueSystem primary = new LeagueSystem();
            primary.setId("main-system");
            primary.setPrimary(true);
            when(leagueSystems.findAll()).thenReturn(List.of(primary));
            Season season = new Season();
            season.setId("season-6");
            season.setLeagueSystemId("main-system");
            season.setNumber(6);
            when(seasons.findByLeagueSystemIdOrderBySequenceAsc("main-system"))
                    .thenReturn(List.of(season));
            RegisteredSource source = new RegisteredSource();
            source.setSourceType(EntityType.Competition);
            source.setSourceEntityId(new SimpleIdentity("cup-6", 3));
            source.setSeasonId("season-6");
            when(registeredSources.findByLeagueSystemId("main-system")).thenReturn(List.of(source));
            when(stageSources.findByLeagueSystemId("main-system")).thenReturn(List.of());
        when(providerRouter.planForTask(
            eq(DedicatedFanAiProfileGenerator.AGENT_ID),
            eq(ContextTaskType.FAN_PROFILE),
            any(LlmProviderRouter.ExecutionOverrides.class)))
            .thenReturn(new LlmProviderRouter.ExecutionPlan(
                new LlmProviderRouter.ModelTarget(
                    "copilot-gpt-6-luna", "copilot", "model", "copilot"),
                List.of(),
                50));
        Instant cooldownUntil = Instant.now().plusSeconds(900);
        providerCooldowns.extend("copilot", cooldownUntil);

        var summary = service.enqueueAll("copilot-gpt-6-luna");

        assertThat(summary.queuedTeams()).isEqualTo(1);
        verify(jobs).save(org.mockito.ArgumentMatchers.argThat(job ->
            "copilot-gpt-6-luna".equals(job.getTargetOverride())
                && job.getPriority() == 17
                && !job.getRequestedAt().isBefore(cooldownUntil)));
        }

    private static Team team(long id, Integer desiredFans) {
        Team team = new Team(new SimpleIdentity("team-" + id, 3));
        team.setDedicatedFans(desiredFans);
        return team;
    }

    private static AiCommunityMemberProfile profile(String teamId, boolean active) {
        AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
        profile.setTeamId(teamId);
        profile.setActive(active);
        return profile;
    }
}
