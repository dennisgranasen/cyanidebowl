package de.dbbcev.dbbcbb3facade.service;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.StatusRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.StatusResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.competitions.CompetitionsRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.competitions.CompetitionsResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.contests.ContestsRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.contests.ContestsResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.leagues.LeagueRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.leagues.LeagueResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup.LookupRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup.LookupResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.matches.MatchesRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.matches.MatchesResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.teams.TeamRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.teams.TeamsRequest;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.teams.TeamsResponse;
import de.dbbcev.dbbcbb3facade.domain.CompetitionRepository;
import de.dbbcev.dbbcbb3facade.domain.ContestRepository;
import de.dbbcev.dbbcbb3facade.domain.LeagueRepository;
import de.dbbcev.dbbcbb3facade.domain.MatchRepository;
import de.dbbcev.dbbcbb3facade.domain.StatusRepository;
import de.dbbcev.dbbcbb3facade.domain.TeamRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import de.dbbcev.dbbcbb3facade.domain.model.Contest;
import de.dbbcev.dbbcbb3facade.domain.model.League;
import de.dbbcev.dbbcbb3facade.domain.model.Match;
import de.dbbcev.dbbcbb3facade.domain.model.Status;
import de.dbbcev.dbbcbb3facade.domain.model.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CyanideApiService {
    public static final String BB3_GAME_NAME = "Blood Bowl III";

    private final CyanideCachedRestApiClient cyanideCachedRestApiClient;

    private final CyanideModelConverter cyanideModelConverter;

    private final StatusRepository statusRepository;

    private final TeamRepository teamRepository;

    private final MatchRepository matchRepository;

    private final ContestRepository contestRepository;

    private final LeagueRepository leagueRepository;

    private final CompetitionRepository competitionRepository;

    public void loadLeague(UUID leagueId) {
        LeagueRequest leagueRequest = new LeagueRequest();
        leagueRequest.setLeague_id(leagueId);
        LeagueResponse leagueResponse = cyanideCachedRestApiClient.getFromCacheOrApi(leagueRequest);
        League league = cyanideModelConverter.toLeague(leagueResponse.getLeague());
        if (league != null) {
            leagueRepository.save(league);
        }
    }

    public void loadTeams(UUID competitionId) {
        TeamsRequest teamsRequest = new TeamsRequest();
        teamsRequest.setCompetition_id(competitionId);
        TeamsResponse teamsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamsRequest);
        List<Team> teams = cyanideModelConverter.toTeams(teamsResponse);
        teamRepository.saveAll(teams);

        teams = teams.stream()
                .map(this::createTeamRequestFor)
                .map(cyanideCachedRestApiClient::getFromCacheOrApi)
                .map(cyanideModelConverter::toTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        teamRepository.saveAll(teams);
    }

    public List<Competition> loadCompetitions(UUID leagueId) {
        CompetitionsRequest competitionsRequest = new CompetitionsRequest();
        competitionsRequest.setLeague_id(leagueId);
        CompetitionsResponse competitionsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(competitionsRequest);
        List<Competition> competitions = cyanideModelConverter.toCompetitions(competitionsResponse);
        return competitionRepository.saveAll(competitions);
    }

    public void loadMatches(UUID leagueId) {
        MatchesRequest matchesRequest = new MatchesRequest();
        matchesRequest.setLeague_id(leagueId);
        MatchesResponse matchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchesRequest);
        List<Match> matches = cyanideModelConverter.toMatches(matchesResponse);
        matchRepository.saveAll(matches);
    }

    public void loadContests(Competition competition) {
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setCompetition_id(competition.getUuid());
        contestsRequest.setLeague_id(competition.getLeagueId());
        if (competition.getRoundsCount() == null) {
            loadContests(contestsRequest);
        } else {
            for (int round = 1; round < competition.getRoundsCount(); round++) {
                contestsRequest.setRound(round);
                loadContests(contestsRequest);
            }
        }
    }

    private void loadContests(ContestsRequest contestsRequest) {
        for (ContestsRequest.Status status : ContestsRequest.Status.values()) {
            contestsRequest.setStatus(status);
            ContestsResponse contestsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(contestsRequest);
            List<Contest> contests = cyanideModelConverter.toContests(contestsResponse);
            contestRepository.saveAll(contests);
        }
    }

    private TeamRequest createTeamRequestFor(Team team) {
        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setId(team.getId());
        return teamRequest;
    }

    public LookupResponse lookup(LookupRequest lookupRequest) {
        LookupResponse lookupResponse = cyanideCachedRestApiClient.getFromCacheOrApi(lookupRequest);
        return lookupResponse;
    }

    public void checkApiStatus() {
        Status status;
        try {
            Optional<StatusResponse> statusResponse = Optional.ofNullable(
                    cyanideCachedRestApiClient.getFromCacheOrApi(new StatusRequest()));
            status = statusResponse
                    .map(sr -> Arrays.stream(sr.getGames()))
                    .orElse(Stream.empty())
                    .filter(game -> BB3_GAME_NAME.equals(game.getName()))
                    .findFirst()
                    .map(cyanideModelConverter::toStatus)
                    .orElse(createEmptyStatus());
        } catch (ResourceAccessException ex) {
            status = createEmptyStatus();
        }
        status.setLastCheck(new Date());
        log.info("Current status is {}.", status);
        statusRepository.save(status);
    }

    private Status createEmptyStatus() {
        Status status = new Status();
        status.setGameName(BB3_GAME_NAME);
        return status;
    }

}
