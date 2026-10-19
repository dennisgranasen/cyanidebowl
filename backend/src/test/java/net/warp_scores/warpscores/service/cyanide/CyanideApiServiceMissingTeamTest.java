package net.warp_scores.warpscores.service.cyanide;

import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.requests.MatchRequest;
import net.warp_scores.warpscores.cyanide.api.requests.TeamRequest;
import net.warp_scores.warpscores.cyanide.api.responses.MatchResponse;
import net.warp_scores.warpscores.cyanide.api.responses.MatchesResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.domain.CompetitionDomainService;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.domain.LeagueDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamCollectionDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.service.StatusModelConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CyanideApiServiceMissingTeamTest {
    @Mock private CyanideCachedRestApiClient cachedRestApiClient;
    @Mock private CyanideRestApiClient restApiClient;
    @Mock private StatusRepository statusRepository;
    @Mock private StatusModelConverter statusModelConverter;
    @Mock private TeamDomainService teamDomainService;
    @Mock private MatchDomainService matchDomainService;
    @Mock private ContestDomainService contestDomainService;
    @Mock private LeagueDomainService leagueDomainService;
    @Mock private CompetitionDomainService competitionDomainService;
    @Mock private TeamCollectionDomainService teamCollectionDomainService;
    @Mock private ContestRepository contestRepository;

    @InjectMocks private CyanideApiService cyanideApiService;

    @Test
    void loadMatchFetchesTeamMissingFromTeamRepository() {
        ApiTeam reportedTeam = team("team-1");
        MatchResponse matchResponse = new MatchResponse();
        matchResponse.setTeams(new ApiTeam[] { reportedTeam });
        TeamResponse teamResponse = new TeamResponse();
        teamResponse.setTeam(team("team-1"));
        Match expectedMatch = new Match(new SimpleIdentity("match-1", 3));

        when(cachedRestApiClient.getFromCacheOrApi(any(MatchRequest.class))).thenReturn(matchResponse);
        when(cachedRestApiClient.getFromCacheOrApi(any(TeamRequest.class))).thenReturn(teamResponse);
        when(teamDomainService.exists(new SimpleIdentity("team-1", 3))).thenReturn(false);
        when(matchDomainService.createOrUpdateMatch(matchResponse, 3)).thenReturn(expectedMatch);

        assertSame(expectedMatch, cyanideApiService.loadMatch("match-1", 3));

        verify(cachedRestApiClient).getFromCacheOrApi(any(TeamRequest.class));
        verify(teamDomainService).createOrUpdateTeam(teamResponse, 3);
    }

    @Test
    void loadMatchesFetchesMissingTeamsFromMatchSummaries() {
        ApiMatch reportedMatch = new ApiMatch();
        reportedMatch.setTeams(new ApiTeam[] { team("team-2") });
        MatchesResponse matchesResponse = new MatchesResponse();
        matchesResponse.setMatches(new ApiMatch[] { reportedMatch });
        TeamResponse teamResponse = new TeamResponse();
        teamResponse.setTeam(team("team-2"));
        when(cachedRestApiClient.getFromCacheOrApi(any())).thenAnswer(invocation -> {
            Object request = invocation.getArgument(0);
            if (request instanceof TeamRequest) {
                return teamResponse;
            }
            return matchesResponse;
        });
        when(teamDomainService.exists(new SimpleIdentity("team-2", 3))).thenReturn(false);
        when(matchDomainService.createOrUpdateMatches(matchesResponse, 3)).thenReturn(List.of());

        cyanideApiService.loadMatches(
                new SimpleIdentity("league-1", 3), EntityType.League,
                Optional.of(new Date(0)), Optional.empty(), Optional.empty(), Optional.empty());

        verify(cachedRestApiClient).getFromCacheOrApi(any(TeamRequest.class));
        verify(teamDomainService).createOrUpdateTeam(teamResponse, 3);
    }

    @Test
    void loadMatchDoesNotFetchTeamAlreadyInRepository() {
        MatchResponse matchResponse = new MatchResponse();
        matchResponse.setTeams(new ApiTeam[] { team("team-3") });
        when(cachedRestApiClient.getFromCacheOrApi(any(MatchRequest.class))).thenReturn(matchResponse);
        when(teamDomainService.exists(new SimpleIdentity("team-3", 3))).thenReturn(true);

        cyanideApiService.loadMatch("match-2", 3);

        verify(cachedRestApiClient, never()).getFromCacheOrApi(any(TeamRequest.class));
    }

    private static ApiTeam team(String id) {
        ApiTeam team = new ApiTeam();
        team.setId(id);
        return team;
    }
}