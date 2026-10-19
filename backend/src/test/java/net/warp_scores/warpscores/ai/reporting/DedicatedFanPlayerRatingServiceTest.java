package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import net.warp_scores.warpscores.service.LocalizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DedicatedFanPlayerRatingServiceTest {
    private final AiCommunityMemberProfileRepository profiles =
            mock(AiCommunityMemberProfileRepository.class);
    private final AiPlayerMatchRatingRepository ratings =
            mock(AiPlayerMatchRatingRepository.class);
    private final DedicatedFanPlayerRatingService service = new DedicatedFanPlayerRatingService(
            profiles,
            new ObjectMapper(),
            mock(ContextPlanner.class),
            mock(ContextAssemblyService.class),
            mock(LlmExecutionService.class),
            ratings,
            mock(LocalizationService.class));

    @Test
    void unratedActiveFansIncludesOnlyInterestedFansWithoutAnExistingMatchRating() {
        AiCommunityMemberProfile alreadyRated = fan("rated", "3_team-a", 1L);
        AiCommunityMemberProfile stillUnrated = fan("unrated", "team-b", 2L);
        AiCommunityMemberProfile unrelated = fan("unrelated", "team-c", 3L);
        when(profiles.findByActiveTrueOrderByTeamIdAscOrdinalAsc())
                .thenReturn(List.of(alreadyRated, stillUnrated, unrelated));
        AiPlayerMatchRating existingRating = new AiPlayerMatchRating();
        existingRating.setReporterId("fan:rated");
        when(ratings.findByMatchId("match-1")).thenReturn(List.of(existingRating));

        var facts = PlayerRatingFacts.builder()
                .matchId("match-1")
                .players(List.of(
                        PlayerRatingFacts.Player.builder().teamId("team-a").build(),
                        PlayerRatingFacts.Player.builder().teamId("3_team-b").build()))
                .matchSummary(Map.of())
                .build();

        assertThat(service.unratedActiveFansFor(facts))
                .extracting(AiCommunityMemberProfile::getId)
                .containsExactly("unrated");
        verify(ratings).findByMatchId("match-1");
        verify(ratings, never()).findByMatchIdAndReporterId(anyString(), anyString());
    }

    private static AiCommunityMemberProfile fan(String id, String teamId, long userId) {
        AiCommunityMemberProfile fan = new AiCommunityMemberProfile();
        fan.setId(id);
        fan.setTeamId(teamId);
        fan.setUserId(userId);
        fan.setUserSubject("subject-" + userId);
        fan.setActive(true);
        return fan;
    }
}
