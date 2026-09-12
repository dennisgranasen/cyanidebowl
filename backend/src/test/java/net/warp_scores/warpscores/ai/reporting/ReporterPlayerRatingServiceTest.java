package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReporterPlayerRatingServiceTest {
    private final AiReporterEffectiveProfileService profiles =
            mock(AiReporterEffectiveProfileService.class);
    private final AiPlayerMatchRatingRepository ratings =
            mock(AiPlayerMatchRatingRepository.class);
    private final ReporterPlayerRatingService.PlayerRatingGenerator generator =
            mock(ReporterPlayerRatingService.PlayerRatingGenerator.class);

    private final ReporterPlayerRatingService service =
            new ReporterPlayerRatingService(profiles, ratings, generator);

    @Test
    void emptyReporterSelectionRunsAllEnabledRatingAgentsInBulk() {
        AiReporterDefinition a = reporter("a");
        AiReporterDefinition b = reporter("b");
        when(profiles.enabledForRatings()).thenReturn(List.of(effective(a), effective(b)));
        when(ratings.findByMatchIdAndReporterId(anyString(), anyString()))
                .thenReturn(List.of());

        PlayerRatingFacts facts = PlayerRatingFacts.builder()
                .schemaVersion("v1")
                .matchId("match-1")
                .players(List.of(
                        PlayerRatingFacts.Player.builder().playerId("p1").build(),
                        PlayerRatingFacts.Player.builder().playerId("p2").build()))
                .build();

        List<String> selected = service.rateMatch(
                facts, List.of(), "Prioritera avgörande insatser", false);

        assertEquals(List.of("a", "b"), selected);
        verify(generator).generateAndPersist(
                eq(a), same(facts), eq("Prioritera avgörande insatser"));
        verify(generator).generateAndPersist(
                eq(b), same(facts), eq("Prioritera avgörande insatser"));
    }

    @Test
    void forceRegeneratesEvenWhenReporterAlreadyHasRatings() {
        AiReporterDefinition a = reporter("a");
        when(profiles.enabledForRatings()).thenReturn(List.of(effective(a)));
        when(ratings.findByMatchIdAndReporterId("match-1", "a"))
                .thenReturn(List.of(new AiPlayerMatchRating()));

        PlayerRatingFacts facts = PlayerRatingFacts.builder()
                .schemaVersion("v1")
                .matchId("match-1")
                .players(List.of())
                .build();

        service.rateMatch(facts, List.of("a"), null, true);

        verify(generator).generateAndPersist(eq(a), same(facts), isNull());
    }

    @Test
    void rejectsUnknownOrDisabledReporterIds() {
        AiReporterDefinition a = reporter("a");
        when(profiles.enabledForRatings()).thenReturn(List.of(effective(a)));

        PlayerRatingFacts facts = PlayerRatingFacts.builder()
                .schemaVersion("v1").matchId("match-1").players(List.of()).build();

        assertThrows(IllegalArgumentException.class,
                () -> service.rateMatch(facts, List.of("missing"), null, false));

        verifyNoInteractions(generator);
    }

    private static AiReporterDefinition reporter(String id) {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId(id);
        reporter.setAlias(id);
        reporter.setUserId(42L);
        return reporter;
    }

    private static AiReporterEffectiveProfileService.EffectiveReporter effective(
            AiReporterDefinition reporter) {
        return new AiReporterEffectiveProfileService.EffectiveReporter(
                reporter, true, true, true, true, 1.0, "sv");
    }
}
