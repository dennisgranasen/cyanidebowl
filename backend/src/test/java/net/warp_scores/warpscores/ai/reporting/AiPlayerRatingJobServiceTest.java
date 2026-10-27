package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.AiProviderWorkExecutor;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerRatingJobRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerRatingTaskRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiPlayerRatingJobServiceTest {
    @Mock private AiReporterEffectiveProfileService effectiveProfiles;
    @Mock private ReporterPlayerRatingService.PlayerRatingGenerator generator;
    @Mock private AiPlayerMatchRatingRepository ratings;
    @Mock private AiPlayerRatingJobRepository jobs;
    @Mock private AiPlayerRatingTaskRepository tasks;
    @Mock private ReplayAnalysisRepository replayAnalyses;
    @Mock private PlayerRatingFactsBuilder factsBuilder;
    @Mock private CopilotRatingQuotaUsageService quotaUsage;
    @Mock private LlmProviderRouter providerRouter;
    @Mock private AiProviderWorkExecutor providerWorkExecutor;
        @Mock private DedicatedFanPlayerRatingJobService fanRatingJobs;
        @Mock private AiPlayerRatingTargetSettings targetSettings;
    @InjectMocks private AiPlayerRatingJobService jobService;

    @Test
    void defaultReporterSelectionRandomizesTenEnabledCandidates() {
        List<AiReporterDefinition> reporters = IntStream.range(0, 20)
                .mapToObj(index -> reporter("reporter-" + index))
                .toList();

        List<AiReporterDefinition> selected =
                AiPlayerRatingJobService.randomSelection(reporters, 10, new Random(1234));

        assertEquals(10, selected.size());
        assertEquals(10, selected.stream().map(AiReporterDefinition::getId).distinct().count());
        Set<String> eligibleIds = reporters.stream()
                .map(AiReporterDefinition::getId)
                .collect(Collectors.toSet());
        assertTrue(selected.stream().allMatch(reporter -> eligibleIds.contains(reporter.getId())));
        assertNotEquals(reporters.subList(0, 10), selected);
    }

    @Test
    void randomSelectionUsesAllCandidatesWhenFewerThanLimit() {
        List<AiReporterDefinition> reporters = List.of(reporter("one"), reporter("two"));

        assertEquals(2, AiPlayerRatingJobService.randomSelection(
                reporters, 10, new Random(1234)).size());
    }

    @Test
    void topUpCountsExistingRatingsAndActiveTasksAndStopsAtTen() {
        List<AiReporterDefinition> reporters = IntStream.range(0, 15)
                .mapToObj(index -> reporter("reporter-" + index))
                .toList();
        Set<String> rated = IntStream.range(0, 7)
                .mapToObj(index -> "reporter-" + index)
                .collect(Collectors.toSet());
        Set<String> active = Set.of("reporter-7", "reporter-8");

        List<AiReporterDefinition> selected = AiPlayerRatingJobService.additionalReporters(
                reporters, rated, active, 10, new Random(4321));

        assertEquals(1, selected.size());
        assertTrue(selected.stream().noneMatch(reporter ->
                rated.contains(reporter.getId()) || active.contains(reporter.getId())));
        assertTrue(AiPlayerRatingJobService.additionalReporters(
                reporters, rated, Set.of("reporter-7", "reporter-8", "reporter-9"),
                10, new Random(4321)).isEmpty());
    }

    @Test
    void automaticTopUpDoesNothingWhenTenEditorialReportersAlreadyRated() {
        when(replayAnalyses.findById("match-1")).thenReturn(java.util.Optional.of(new ReplayAnalysis()));
        PlayerRatingFacts facts = PlayerRatingFacts.builder()
                .matchId("match-1")
                .players(List.of(PlayerRatingFacts.Player.builder().playerId("player-1").build()))
                .build();
        when(factsBuilder.build(any())).thenReturn(facts);
        List<AiPlayerMatchRating> existingRatings = IntStream.range(0, 10)
                .mapToObj(index -> {
                    AiPlayerMatchRating rating = new AiPlayerMatchRating();
                    rating.setReporterId("reporter-" + index);
                    rating.setSourceType(AiPlayerMatchRating.SourceType.EDITORIAL);
                    return rating;
                })
                .toList();
        when(ratings.findByMatchId("match-1")).thenReturn(existingRatings);

        jobService.queueAfterReplayAnalysis("match-1");

        verifyNoInteractions(tasks, jobs, effectiveProfiles, providerRouter, providerWorkExecutor);
        verify(fanRatingJobs).queueAfterReplayAnalysis("match-1", facts);
    }

    @Test
    void newEditorialRatingTasksUseConfiguredDefaultTarget() {
        AiReporterDefinition reporter = reporter("reporter-1");
        when(effectiveProfiles.enabledForRatings()).thenReturn(List.of(
                new AiReporterEffectiveProfileService.EffectiveReporter(
                        reporter, true, true, true, true, 1.0, "sv")));
        when(targetSettings.editorialTarget()).thenReturn("editorial-target");
        when(providerRouter.planForTask(eq("reporter-1"), eq(ContextTaskType.PLAYER_RATING), any()))
                .thenReturn(new LlmProviderRouter.ExecutionPlan(
                        new LlmProviderRouter.ModelTarget("editorial-target", "provider", "model", "quota"),
                        List.of(), 50));

        jobService.start("match-1", List.of("reporter-1"), null, false, 1, "admin", null);

        ArgumentCaptor<List<net.warp_scores.warpscores.model.AiPlayerRatingTask>> tasksCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(tasks).saveAll(tasksCaptor.capture());
        assertEquals("editorial-target", tasksCaptor.getValue().get(0).getTargetOverride());
        verify(quotaUsage).start(anyString(), eq("match-1"),
                eq(net.warp_scores.warpscores.model.CopilotRatingQuotaUsage.RatingKind.EDITORIAL),
                eq("admin"), eq("editorial-target"), eq(1));
    }

    private static AiReporterDefinition reporter(String id) {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId(id);
        return reporter;
    }
}