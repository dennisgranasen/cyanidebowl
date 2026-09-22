package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchDetailsBackfillServiceTest {
    private MatchRepository repository;
    private CyanideApiService cyanideApiService;
    private MatchDetailsBackfillService service;

    @BeforeEach
    void setUp() {
        repository = mock(MatchRepository.class);
        cyanideApiService = mock(CyanideApiService.class);
        service = new MatchDetailsBackfillService(repository, cyanideApiService);
    }

    @Test
    void marksExistingPlayerDataWithoutCallingCyanide() {
        Match match = match("newest", true);
        when(repository.findMatchesWithUncheckedDetails(any(Date.class), any(Pageable.class)))
                .thenReturn(List.of(match));

        MatchDetailsBackfillService.BackfillResult result =
                service.improveNewestUnchecked(5, Duration.ofHours(24));

        assertThat(result.available()).isEqualTo(1);
        assertThat(match.getDetailsStatus()).isEqualTo(Match.DetailsStatus.PLAYER_DATA_AVAILABLE);
        assertThat(match.getDetailsCheckedAt()).isNotNull();
        verify(cyanideApiService, never()).loadMatch(any(), anyInt());
        verify(repository).save(match);
    }

    @Test
    void remembersWhenCyanideHasNoPlayerData() {
        Match skeleton = match("skeleton", false);
        Match detailedResponse = match("skeleton", false);
        when(repository.findMatchesWithUncheckedDetails(any(Date.class), any(Pageable.class)))
                .thenReturn(List.of(skeleton));
        when(cyanideApiService.loadMatch("external-skeleton", 3)).thenReturn(detailedResponse);

        MatchDetailsBackfillService.BackfillResult result =
                service.improveNewestUnchecked(5, Duration.ZERO);

        assertThat(result.unavailable()).isEqualTo(1);
        assertThat(detailedResponse.getDetailsStatus()).isEqualTo(Match.DetailsStatus.PLAYER_DATA_UNAVAILABLE);
        assertThat(detailedResponse.getDetailsCheckedAt()).isNotNull();
        verify(repository).save(detailedResponse);
    }

    @Test
    void leavesTransientFailuresUncheckedForRetry() {
        Match skeleton = match("retry", false);
        when(repository.findMatchesWithUncheckedDetails(any(Date.class), any(Pageable.class)))
                .thenReturn(List.of(skeleton));
        when(cyanideApiService.loadMatch("external-retry", 3)).thenThrow(new IllegalStateException("offline"));

        MatchDetailsBackfillService.BackfillResult result =
                service.improveNewestUnchecked(5, Duration.ZERO);

        assertThat(result.failed()).isEqualTo(1);
        assertThat(skeleton.getDetailsStatus()).isNull();
        verify(repository, never()).save(any(Match.class));
    }

    @Test
    void fetchesRecentDetailsButDoesNotPermanentlyMarkMissingData() {
        Match skeleton = match("recent", false);
        Match detailedResponse = match("recent", false);
        when(repository.findMatchesWithUncheckedDetails(any(Date.class), any(Pageable.class)))
                .thenReturn(List.of(skeleton));
        when(cyanideApiService.loadMatch("external-recent", 3)).thenReturn(detailedResponse);

        MatchDetailsBackfillService.BackfillResult result =
                service.improveNewestUnchecked(20, Duration.ofHours(24));

        assertThat(result.unavailable()).isZero();
        assertThat(detailedResponse.getDetailsStatus()).isNull();
        verify(repository, never()).save(any(Match.class));
    }

    private Match match(String id, boolean withPlayers) {
        Match match = new Match(new SimpleIdentity(id, 3));
        match.setMatchId("external-" + id);
        match.setFinished(new Date());
        Team home = new Team(new SimpleIdentity(id + "-home", 3));
        Team away = new Team(new SimpleIdentity(id + "-away", 3));
        if (withPlayers) {
            home.setPlayers(new Player[] { new Player(new SimpleIdentity(id + "-home-player", 3)) });
            away.setPlayers(new Player[] { new Player(new SimpleIdentity(id + "-away-player", 3)) });
        }
        match.setTeams(new Team[] { home, away });
        return match;
    }
}
