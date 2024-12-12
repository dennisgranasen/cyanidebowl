package net.warp_scores.warpscores.service.cyanide;

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
import net.warp_scores.warpscores.domain.CompetitionTeamsDomainService;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.domain.LeagueDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Status;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.StatusModelConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.warp_scores.warpscores.cyanide.api.requests.StatusRequest.BB3_GAME_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class CyanideApiService {
    private final CyanideCachedRestApiClient cyanideCachedRestApiClient;

    private final CyanideRestApiClient cyanideRestApiClient;

    private final StatusRepository statusRepository;

    private final StatusModelConverter statusModelConverter;

    private final TeamDomainService teamDomainService;

    private final MatchDomainService matchDomainService;

    private final ContestDomainService contestDomainService;

    private final LeagueDomainService leagueDomainService;

    private final CompetitionDomainService competitionDomainService;

    private final CompetitionTeamsDomainService competitionTeamsDomainService;
    private final ContestRepository contestRepository;

    public LookupResponse lookup(LookupRequest lookupRequest) {
        return cyanideCachedRestApiClient.getFromCacheOrApi(lookupRequest);
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

        competitionTeamsDomainService.createOrUpdateCompetitionTeams(competition, teams);
        return teams;
    }

    public void loadTeamMatches(Team team, Optional<Date> earliestStartDate, Optional<Date> lastMatchDateKnown,
            Optional<Date> lastMatchDateReported) {
        if (team == null || team.getId() == null) {
            return;
        }
        log.info(
                "Checking if matches to be loaded for team {} (earliestStart: {}, lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                team.getId(), earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
        Date startDate = lastMatchDateKnown.orElse(earliestStartDate.orElse(null));
        if (startDate == null || (lastMatchDateReported.isPresent() && !startDate.before(
                lastMatchDateReported.get()))) {
            log.info("No matches to load for team {}.", team.getId());
            return;
        }

        TeamMatchesRequest teamMatchesRequest = new TeamMatchesRequest();
        teamMatchesRequest.setTeam(team.getId());
        teamMatchesRequest.setStart(startDate);
        log.info(
                "Loading matches for team {} starting from {}.",
                team.getId(), startDate);
        TeamMatchesResponse teamMatchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamMatchesRequest);
        List<UUID> matchUuids = ofNullable(teamMatchesResponse)
                .stream()
                .flatMap(t -> Arrays.stream(
                        ofNullable(t.getMatchIds())
                                .orElse(new TeamMatchesResponse.MatchId[0])))
                .map(TeamMatchesResponse.MatchId::getUuid)
                .toList();
        List<Match> matches = matchUuids
                .stream()
                .filter(Objects::nonNull)
                .map(this::loadMatch)
                .toList();
        log.info("Loaded {} matches for team {}.", matches.size(), team.getId());
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

    public List<Match> loadMatches(League league,
            Optional<Date> earliestStartDate,
            Optional<Date> lastMatchDateKnown,
            Optional<Date> lastMatchDateReported) {
        log.info(
                "Checking if matches to be loaded for league {} (earliestStart: {}, lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                league.getUuid(), earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
        Date startDate = lastMatchDateKnown.orElse(earliestStartDate.orElse(null));
        if (startDate != null && (lastMatchDateReported.isEmpty() || startDate.before(lastMatchDateReported.get()))) {
            MatchesRequest matchesRequest = new MatchesRequest();
            matchesRequest.setLeague_id(league.getUuid());
            matchesRequest.setStart(startDate);
            log.info(
                    "Loading matches for league {} starting from {}.",
                    league.getUuid(), startDate);
            MatchesResponse matchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchesRequest);
            return matchDomainService.createOrUpdateMatches(matchesResponse);
        }
        log.info("No matches to load for league {}.", league.getUuid());
        return Collections.emptyList();
    }

    public List<Competition> loadCompetitions(UUID leagueId) {
        CompetitionsRequest competitionsRequest = new CompetitionsRequest();
        competitionsRequest.setLeague_id(leagueId);
        CompetitionsResponse competitionsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(competitionsRequest);
        return competitionDomainService.createOrUpdateCompetitions(competitionsResponse);
    }

    public List<Contest> loadContests(Competition competition) {
        Integer contestCount = contestRepository.countByCompetitionId(competition.getUuid());
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setCompetition_id(competition.getUuid());

        contestsRequest.setStatus("*");
        contestsRequest.setLimitOffset(contestCount);
        return new ArrayList<>(loadContests(contestsRequest));
    }

    private List<Contest> loadContests(ContestsRequest contestsRequest) {
        ContestsResponse contestsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(contestsRequest);
        return contestDomainService.createOrUpdateContests(contestsResponse);
    }

    @Transactional
    public void checkApiStatus() {
        Status status;
        try {

            Optional<StatusResponse> statusResponse = ofNullable(cyanideRestApiClient.loadFromApi(new StatusRequest()));
            status = statusResponse
                    .stream()
                    .flatMap(sr -> Arrays.stream(sr.getGames()))
                    .filter(game -> BB3_GAME_NAME.equals(game.getName()))
                    .findFirst()
                    .map(statusModelConverter::toStatus)
                    .orElse(createEmptyStatus());
        } catch (ResourceAccessException ex) {
            status = createEmptyStatus();
        }
        status.setLastCheck(new Date());
        log.info("Current status is (overall={}, serviceStatuses={}, maintenance={}).", status.isOverall(),
                status.getServiceStatuses(), status.getMaintenance());
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
