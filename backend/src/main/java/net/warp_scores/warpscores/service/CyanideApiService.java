package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.requests.CompetitionsRequest;
import net.warp_scores.warpscores.cyanide.api.requests.ContestsRequest;
import net.warp_scores.warpscores.cyanide.api.requests.LeagueRequest;
import net.warp_scores.warpscores.cyanide.api.requests.LookupRequest;
import net.warp_scores.warpscores.cyanide.api.requests.MatchRequest;
import net.warp_scores.warpscores.cyanide.api.requests.MatchesRequest;
import net.warp_scores.warpscores.cyanide.api.requests.StatusRequest;
import net.warp_scores.warpscores.cyanide.api.requests.TeamMatchesRequest;
import net.warp_scores.warpscores.cyanide.api.requests.TeamRequest;
import net.warp_scores.warpscores.cyanide.api.requests.TeamsRequest;
import net.warp_scores.warpscores.cyanide.api.responses.CompetitionsResponse;
import net.warp_scores.warpscores.cyanide.api.responses.ContestsResponse;
import net.warp_scores.warpscores.cyanide.api.responses.LeagueResponse;
import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import net.warp_scores.warpscores.cyanide.api.responses.MatchResponse;
import net.warp_scores.warpscores.cyanide.api.responses.MatchesResponse;
import net.warp_scores.warpscores.cyanide.api.responses.StatusResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamMatchesResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamsResponse;
import net.warp_scores.warpscores.domain.CompetitionDomainService;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.domain.LeagueDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.model.Contest;
import net.warp_scores.warpscores.domain.model.League;
import net.warp_scores.warpscores.domain.model.Match;
import net.warp_scores.warpscores.domain.model.Status;
import net.warp_scores.warpscores.domain.model.Team;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CyanideApiService {
    public static final String BB3_GAME_NAME = "Blood Bowl III";

    private final CyanideCachedRestApiClient cyanideCachedRestApiClient;

    private final StatusRepository statusRepository;

    private final StatusModelConverter statusModelConverter;

    private final TeamDomainService teamDomainService;

    private final MatchDomainService matchDomainService;

    private final ContestDomainService contestDomainService;

    private final LeagueDomainService leagueDomainService;

    private final CompetitionDomainService competitionDomainService;

    public LookupResponse lookup(LookupRequest lookupRequest) {
        LookupResponse lookupResponse = cyanideCachedRestApiClient.getFromCacheOrApi(lookupRequest);
        return lookupResponse;
    }

    public League loadLeague(UUID leagueId) {
        LeagueRequest leagueRequest = new LeagueRequest();
        leagueRequest.setLeague_id(leagueId);
        LeagueResponse leagueResponse = cyanideCachedRestApiClient.getFromCacheOrApi(leagueRequest);
        return leagueDomainService.createOrUpdateLeague(leagueResponse);
    }

    public List<Team> loadTeams(Competition competition) {
        TeamsRequest teamsRequest = new TeamsRequest();
        teamsRequest.setCompetition_id(competition.getUuid());
        teamsRequest.setLeague_id(competition.getLeagueId());
        TeamsResponse teamsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamsRequest);
        List<Team> teams = teamDomainService.createOrUpdateTeams(teamsResponse);

        teams = teams.stream()
                .map(this::createTeamRequestFor)
                .map(cyanideCachedRestApiClient::getFromCacheOrApi)
                .map(teamDomainService::createOrUpdateTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return teams;
    }

    public List<UUID> loadTeamMatches(UUID teamUuid) {
        if (teamUuid == null) {
            return Collections.emptyList();
        }
        TeamMatchesRequest teamMatchesRequest = new TeamMatchesRequest();
        teamMatchesRequest.setTeamId(teamUuid);
        TeamMatchesResponse teamMatchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamMatchesRequest);
        return ofNullable(teamMatchesResponse)
                .map(t -> Arrays.stream(
                        ofNullable(t.getMatchIds())
                                .orElse(new TeamMatchesResponse.MatchId[0])))
                .orElse(Stream.empty())
                .map(TeamMatchesResponse.MatchId::getUuid)
                .collect(Collectors.toList());
    }

    public Match loadMatch(UUID matchUuid) {
        if (matchUuid == null) {
            return null;
        }
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setMatch_id(matchUuid);
        MatchResponse matchResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchRequest);
        return matchDomainService.createOrUpdateMatch(matchResponse);
    }

    public List<Match> loadMatches(League league, Date earliestStartDate, Optional<Date> lastMatchDate) {
        MatchesRequest matchesRequest = new MatchesRequest();
        matchesRequest.setLeague_id(league.getUuid());
        matchesRequest.setStart(lastMatchDate.orElse(earliestStartDate));
        MatchesResponse matchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchesRequest);
        return matchDomainService.createOrUpdateMatches(matchesResponse);
    }

    public List<Competition> loadCompetitions(UUID leagueId) {
        CompetitionsRequest competitionsRequest = new CompetitionsRequest();
        competitionsRequest.setLeague_id(leagueId);
        CompetitionsResponse competitionsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(competitionsRequest);
        return competitionDomainService.createOrUpdateCompetitions(competitionsResponse);
    }

    public List<Contest> loadContests(Competition competition) {
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setCompetition_id(competition.getUuid());
        contestsRequest.setLeague_id(competition.getLeagueId());
        List<Contest> contests = new ArrayList<>();
        if (competition.getRoundsCount() == null) {
            contests.addAll(loadContests(contestsRequest));
        } else {
            for (int round = 1; round < competition.getRoundsCount(); round++) {
                contestsRequest.setRound(round);
                contests.addAll(loadContests(contestsRequest));
            }
        }
        return contests;
    }

    private List<Contest> loadContests(ContestsRequest contestsRequest) {
        List<Contest> allContests = new ArrayList<>();
        for (ContestsRequest.Status status : ContestsRequest.Status.values()) {
            contestsRequest.setStatus(status);
            ContestsResponse contestsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(contestsRequest);
            allContests.addAll(contestDomainService.createOrUpdateContests(contestsResponse));
        }
        Map<UUID, Contest> latestContests = allContests.stream()
                .collect(toMap(Contest::getContestUuid, Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(Contest::getMatchDate,
                                Comparator.nullsFirst(Comparator.naturalOrder())))));
        allContests = new ArrayList<>(latestContests.values());
        return allContests;
    }

    @Transactional
    public void checkApiStatus() {
        Status status;
        try {
            Optional<StatusResponse> statusResponse = ofNullable(
                    cyanideCachedRestApiClient.getFromCacheOrApi(new StatusRequest()));
            status = statusResponse
                    .map(sr -> Arrays.stream(sr.getGames()))
                    .orElse(Stream.empty())
                    .filter(game -> BB3_GAME_NAME.equals(game.getName()))
                    .findFirst()
                    .map(statusModelConverter::toStatus)
                    .orElse(createEmptyStatus());
        } catch (ResourceAccessException ex) {
            status = createEmptyStatus();
        }
        status.setLastCheck(new Date());
        log.info("Current status is {}.", status);
        statusRepository.save(status);
    }

    private TeamRequest createTeamRequestFor(Team team) {
        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setId(team.getId());
        return teamRequest;
    }

    private Status createEmptyStatus() {
        Status status = new Status();
        status.setGameName(BB3_GAME_NAME);
        return status;
    }
}
