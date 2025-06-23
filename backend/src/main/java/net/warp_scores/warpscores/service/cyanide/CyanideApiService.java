package net.warp_scores.warpscores.service.cyanide;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.requests.*;
import net.warp_scores.warpscores.cyanide.api.responses.*;
import net.warp_scores.warpscores.domain.*;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.*;
import net.warp_scores.warpscores.service.StatusModelConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.stream.Collectors;

import javax.swing.text.html.parser.Entity;

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
    private final TeamCollectionDomainService teamCollectionDomainService;
    private final ContestRepository contestRepository;

    public LookupResponse lookup(LookupRequest lookupRequest) {
        return cyanideCachedRestApiClient.getFromCacheOrApi(lookupRequest);
    }

    public League loadLeague(Identity leagueIdentity) {
        LeagueRequest leagueRequest = new LeagueRequest();
        System.out.println(leagueIdentity.getValue());
        leagueRequest.setLeague_id(leagueIdentity.getValue());
        leagueRequest.setOpus(leagueIdentity.getOpus());
        LeagueResponse leagueResponse = cyanideCachedRestApiClient.getFromCacheOrApi(leagueRequest);
        return leagueDomainService.createOrUpdateLeague(leagueResponse);
    }

    public List<Team> loadTeams(Identity id, EntityType entityType) {
        TeamsRequest teamsRequest = new TeamsRequest();
        if (entityType == EntityType.League) {
            teamsRequest.setLeague_id(id.getValue());
        } else if (entityType == EntityType.Competition && id instanceof CompositeIdentity cid) {
            teamsRequest.setLeague_id(cid.getParts()[0]);
            teamsRequest.setCompetition_id(cid.getParts()[1]);
        } else {
            throw new IllegalArgumentException("Unsupported id/entityType combo: " + id + "::" + entityType);
        }
        teamsRequest.setOpus(id.getOpus());
        TeamsResponse teamsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamsRequest);
        List<Team> teams = teamDomainService.createOrUpdateTeams(teamsResponse);

        teams = teams.stream()
                .map(this::createTeamRequestFor)
                .map(cyanideCachedRestApiClient::getFromCacheOrApi)
                .map(teamDomainService::createOrUpdateTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        teamCollectionDomainService.createOrUpdateTeamCollection(id, entityType, teams);
        return teams;
    }

    public void loadTeamMatches(Team team, Optional<Date> earliestStartDate, Optional<Date> lastMatchDateKnown,
                               Optional<Date> lastMatchDateReported) {
        if (team == null || team.getIdentity() == null) {
            return;
        }
        log.info(
                "Checking if matches to be loaded for team {} (earliestStart: {}, lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                team.getTeamId(), earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
        Date startDate = lastMatchDateKnown.orElse(earliestStartDate.orElse(null));

        if (startDate == null || (lastMatchDateReported.isPresent() && !startDate.before(
                lastMatchDateReported.get()))) {
            log.info("No matches to load for team {}.", team.getTeamId());
            return;
        }

        TeamMatchesRequest teamMatchesRequest = new TeamMatchesRequest();
        teamMatchesRequest.setTeam(team.getTeamId());
        teamMatchesRequest.setOpus(team.getIdentity().getOpus());
        teamMatchesRequest.setStart(startDate);
        teamMatchesRequest.setEnd(new Date());
        log.info(
                "Loading matches for team {} starting from {}.",
                team.getTeamId(), startDate);
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
                .map((uuid) -> loadMatch(new SimpleIdentity(uuid, team.getIdentity().getOpus())))
                .toList();
        log.info("Loaded {} matches for team {}.", matches.size(), team.getTeamId());
    }

    public Match loadMatch(Identity matchIdentity) {
        if (matchIdentity == null) {
            return null;
        }
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setMatch_id(matchIdentity.getValue());
        matchRequest.setOpus(matchIdentity.getOpus());
        MatchResponse matchResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchRequest);
        return matchDomainService.createOrUpdateMatch(matchResponse);
    }

    public List<Match> loadMatches(League league,
                                   Optional<Date> earliestStartDate,
                                   Optional<Date> lastMatchDateKnown,
                                   Optional<Date> lastMatchDateReported) {
        log.info(
                "Checking if matches to be loaded for league {} (earliestStart: {}, lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                league.getLeagueId(), earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
        Date startDate = lastMatchDateKnown.orElse(earliestStartDate.orElse(null));
        if (startDate != null && (lastMatchDateReported.isEmpty() || startDate.before(lastMatchDateReported.get()))) {
            MatchesRequest matchesRequest = new MatchesRequest();
            matchesRequest.setLeague_id(league.getLeagueId());
            matchesRequest.setOpus(league.getIdentity().getOpus());
            matchesRequest.setStart(startDate);
            matchesRequest.setEnd(new Date());
            matchesRequest.setLimitSize(null);
            log.info(
                    "Loading matches for league {} starting from {}.",
                    league.getLeagueId(), startDate);
            MatchesResponse matchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchesRequest);
            return matchDomainService.createOrUpdateMatches(matchesResponse);
        }
        log.info("No matches to load for league {}.", league.getLeagueId());
        return Collections.emptyList();
    }

    public List<Match> loadMatches(Competition competition,
                                   Optional<Date> earliestStartDate,
                                   Optional<Date> lastMatchDateKnown,
                                   Optional<Date> lastMatchDateReported) {
        log.info(
                "Checking if matches to be loaded for league {} competition {} (earliestStart: {}, lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                competition.getLeagueId(), competition.getCompetitionId(), 
                earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
        Date startDate = lastMatchDateKnown.orElse(earliestStartDate.orElse(null));
        if (startDate != null && (lastMatchDateReported.isEmpty() || startDate.before(lastMatchDateReported.get()))) {
            MatchesRequest matchesRequest = new MatchesRequest();
            matchesRequest.setLeague_id(competition.getLeagueId().getValue());
            matchesRequest.setCompetition_id(competition.getCompetitionId());
            matchesRequest.setOpus(competition.getIdentity().getOpus());
            matchesRequest.setStart(startDate);
            matchesRequest.setEnd(new Date());
            matchesRequest.setLimitSize(null);
            log.info(
                    "Loading matches for competition {} starting from {}.",
                    competition.getLeagueId(), startDate);
            MatchesResponse matchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchesRequest);
            return matchDomainService.createOrUpdateMatches(matchesResponse);
        }
        log.info("No matches to load for competition {}.", competition.getLeagueId());
        return Collections.emptyList();
    }


    public List<Competition> loadCompetitions(Identity leagueIdentity) {
        CompetitionsRequest competitionsRequest = new CompetitionsRequest();
        competitionsRequest.setLeague_id(leagueIdentity.getValue());
        competitionsRequest.setOpus(leagueIdentity.getOpus());
        CompetitionsResponse competitionsResponse =
                cyanideCachedRestApiClient.getFromCacheOrApi(competitionsRequest);
        return competitionDomainService.createOrUpdateCompetitions(competitionsResponse);
    }

    public List<Contest> loadContests(Competition competition) {
        Integer contestCount =
                contestRepository.countByCompetitionId(
                        competition.getIdentity());
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setCompetition_id(competition.getCompetitionId());
        contestsRequest.setLeague_id(competition.getLeagueId().getValue());
        contestsRequest.setOpus(competition.getIdentity().getOpus());

        int limitOffset = contestCount - ApiRequest.DEFAULT_FETCH_LIMIT;
        if (limitOffset < 0) {
            limitOffset = 0;
        }
        contestsRequest.setStatus("*");
        contestsRequest.setLimitOffset(limitOffset);
        return new ArrayList<>(loadContests(contestsRequest));
    }

    public List<Contest> loadContests(ContestsRequest contestsRequest) {
        ContestsResponse contestsResponse = cyanideCachedRestApiClient.getFromCacheOrApi(contestsRequest);
        return contestDomainService.createOrUpdateContests(contestsResponse);
    }   

    @Transactional
    public void checkApiStatus() {
        Status status;
        try {

            Optional<StatusResponse> statusResponse = ofNullable(
                    cyanideRestApiClient.loadFromApi(new StatusRequest()));
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
        teamRequest.setId(team.getTeamId());
        teamRequest.setOpus(team.getIdentity().getOpus());
        return teamRequest;
    }

    private Status createEmptyStatus() {
        Status status = new Status();
        status.setGameName(BB3_GAME_NAME);
        return status;
    }
}
