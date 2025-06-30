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
import static net.warp_scores.warpscores.cyanide.api.requests.StatusRequest.BB2_GAME_NAME;
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
        return leagueDomainService.createOrUpdateLeague(leagueResponse, leagueIdentity.getOpus());
    }
    
    public Optional<Team> loadTeam(Identity id, Boolean includeStats, Optional<Boolean> includeRoster) {
        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setId(id.getValue());
        teamRequest.setOpus(id.getOpus());
        teamRequest.setStatistics(includeStats ? 1 : 0);
        if (includeRoster.isPresent()) {
            teamRequest.setRoster(includeRoster.get() ? 1 : 0);
        }
        TeamResponse teamResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamRequest);
        if (teamResponse.isEmpty())
            return Optional.empty();
        return ofNullable(teamDomainService.createOrUpdateTeam(teamResponse, id.getOpus()));
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
        List<Team> teams = teamDomainService.createOrUpdateTeams(teamsResponse, id.getOpus());

        teams = teams.stream()
                .map(this::createTeamRequestFor)
                .map(cyanideCachedRestApiClient::getFromCacheOrApi)
                .map(response -> teamDomainService.createOrUpdateTeam(response, id.getOpus()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        teamCollectionDomainService.createOrUpdateTeamCollection(id, entityType, teams);
        return teams;
    }

    public void loadTeamMatches(Team team, Optional<Date> earliestStartDate, Optional<Date> lastMatchDateKnown,
                               Optional<Date> lastMatchDateReported) {
        if (team == null || team.getId() == null) {
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
        teamMatchesRequest.setOpus(team.getId().getOpus());
        teamMatchesRequest.setStart(startDate);
        teamMatchesRequest.setEnd(new Date());
        log.info(
                "Loading matches for team {} starting from {}.",
                team.getTeamId(), startDate);
        TeamMatchesResponse teamMatchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(teamMatchesRequest);
        List<String> matchIds = ofNullable(teamMatchesResponse)
                .stream()
                .flatMap(t -> Arrays.stream(
                        ofNullable(t.getMatchIds())
                                .orElse(new TeamMatchesResponse.MatchId[0])))
                .map(x -> x.getUuid().toString())
                .toList();
        List<Match> matches = matchIds
                .stream()
                .filter(Objects::nonNull)
                .map(id -> loadMatch(id, team.getId().getOpus()))
                .toList();
        log.info("Loaded {} matches for team {}.", matches.size(), team.getTeamId());
    }

    public Match loadMatch(String matchId, int opus) {
        if (matchId == null) {
            return null;
        }
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setMatch_id(matchId);
        matchRequest.setOpus(opus);
        MatchResponse matchResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchRequest);
        return matchDomainService.createOrUpdateMatch(matchResponse, opus);
    }

    public List<Match> loadMatches(Identity entityId, EntityType entityType,
                                   Optional<Date> earliestStartDate,
                                   Optional<Date> lastMatchDateKnown,
                                   Optional<Date> lastMatchDateReported, 
                                   Optional<Integer> limit) {
        if (entityId == null || entityType == null)
            return Collections.emptyList();        
            
        log.info(
            "Checking if matches to be loaded for entity {} (type: {}, earliestStart: {}, lastMatchDateKnown: {}, lastMatchDateReported: {}).",
            entityId, entityType, earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
        Date startDate = lastMatchDateKnown.orElse(earliestStartDate.orElse(null));
        if (startDate != null && (lastMatchDateReported.isEmpty() || startDate.before(lastMatchDateReported.get()))) {
            MatchesRequest matchesRequest = new MatchesRequest();
            if (entityType == EntityType.League) {
                matchesRequest.setLeague_id(entityId.getValue());
            } else if (entityType == EntityType.Competition && entityId instanceof CompositeIdentity cid) {
                matchesRequest.setLeague_id(cid.getParts()[0]);
                matchesRequest.setCompetition_id(cid.getParts()[1]);
            } else {
                throw new IllegalArgumentException("Unsupported entityId/entityType combo: " + entityId + "::" + entityType);
            }
            matchesRequest.setOpus(entityId.getOpus());
            matchesRequest.setStart(startDate);
            matchesRequest.setEnd(new Date());
            matchesRequest.setLimitSize(/*limit.orElse(ApiRequest.DEFAULT_FETCH_LIMIT)*/ null);
            log.info(
                    "Loading matches for {} {} starting from {}.",
                    entityType, entityId, startDate);
            MatchesResponse matchesResponse = cyanideCachedRestApiClient.getFromCacheOrApi(matchesRequest);
            return matchDomainService.createOrUpdateMatches(matchesResponse, entityId.getOpus());
        }
        log.info("No matches to load for {} {}.", entityType, entityId);
        return Collections.emptyList();
    }


    public List<Match> loadMatches(League league,
                                   Optional<Date> earliestStartDate,
                                   Optional<Date> lastMatchDateKnown,
                                   Optional<Date> lastMatchDateReported,
                                   Optional<Integer> limit) {
        return loadMatches(league.getId(), EntityType.League, earliestStartDate, lastMatchDateKnown, lastMatchDateReported, limit);
    }

    public List<Match> loadMatches(Competition competition,
                                   Optional<Date> earliestStartDate,
                                   Optional<Date> lastMatchDateKnown,
                                   Optional<Date> lastMatchDateReported,
                                   Optional<Integer> limit) {
        return loadMatches(competition.getId(), EntityType.Competition, earliestStartDate, lastMatchDateKnown, lastMatchDateReported, limit);
    }


    public List<Competition> loadCompetitions(Identity leagueIdentity) {
        CompetitionsRequest competitionsRequest = new CompetitionsRequest();
        competitionsRequest.setLeague_id(leagueIdentity.getValue());
        competitionsRequest.setOpus(leagueIdentity.getOpus());
        CompetitionsResponse competitionsResponse =
                cyanideCachedRestApiClient.getFromCacheOrApi(competitionsRequest);
        return competitionDomainService.createOrUpdateCompetitions(competitionsResponse, leagueIdentity.getOpus());
    }

    public List<Contest> loadContests(Competition competition) {        
        Integer contestCount =
                contestRepository.countByCompetitionId(
                        competition.getId());
        ContestsRequest contestsRequest = new ContestsRequest();
        int opus = competition.getId().getOpus();
        if (opus == 2) {
            contestsRequest.setCompetition_name(competition.getName());
            contestsRequest.setLeague_name(competition.getLeagueName());
            contestsRequest.setExact(0);            
        } else if (opus > 2) {
            String cid = competition.getCompetitionId().getValue();
            contestsRequest.setCompetition_id(cid);
            contestsRequest.setLeague_id(competition.getLeagueId().getValue());
        } else {
            throw new IllegalArgumentException("Unsupported opus: " + opus);
        }
        contestsRequest.setBb(competition.getId().getOpus());


        int limitOffset = contestCount - ApiRequest.DEFAULT_FETCH_LIMIT;
        if (limitOffset < 0) {
            limitOffset = 0;
        }
        contestsRequest.setStatus("*");
        contestsRequest.setLimitOffset(limitOffset);
        return new ArrayList<>(loadContests(contestsRequest));
    }

    public List<Contest> loadContests(League league) {
        Integer contestCount =
                contestRepository.countByLeagueId(league.getId());
        ContestsRequest contestsRequest = new ContestsRequest();
        contestsRequest.setLeague_id(league.getId().getValue());
        contestsRequest.setBb(league.getId().getOpus());

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
        return contestDomainService.createOrUpdateContests(contestsResponse, contestsRequest.getBb());
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
                    .filter(game -> BB3_GAME_NAME.equals(game.getName()) || BB2_GAME_NAME.equals(game.getName()))
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
        teamRequest.setOpus(team.getId().getOpus());
        return teamRequest;
    }

    private Status createEmptyStatus() {
        Status status = new Status();
        status.setGameName(BB3_GAME_NAME);
        return status;
    }
}
