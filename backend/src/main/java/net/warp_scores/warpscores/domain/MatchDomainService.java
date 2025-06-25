package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiCoach;
import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.MatchResponse;
import net.warp_scores.warpscores.cyanide.api.responses.MatchesResponse;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.TeamPopulator;
import net.warp_scores.warpscores.service.UUIDConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchDomainService {
    private final MatchRepository matchRepository;
    private final TeamPopulator teamPopulator;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional
    public List<Match> findMatchesForTeam(Identity teamId) {
        List<Match> teamMatches = 
            matchRepository.findMatchesByTeamId(teamId);
        return teamMatches.stream()
                .sorted(Comparator.comparing(Match::getStarted).reversed())
                .collect(Collectors.toList());
    }

    
    @Transactional
    public Map<Identity, Optional<Date>> getLastMatchDatesForCompetitions(
            List<Competition> compData) {

        List<MatchRepository.DateForId> lastMatchDateByCompIds = Collections.emptyList();

        for (Competition competition : compData) {
            //Integer opus = competition.getOpus();
            List<Identity> compIds = competition.getId() != null
                    ? Collections.singletonList(competition.getId())
                    : Collections.emptyList();
            if (!compIds.isEmpty()) {
                lastMatchDateByCompIds.addAll(matchRepository
                        .findLastMatchDateByCompetitionIds(compIds));
            }
        }
        return lastMatchDateByCompIds
                .stream()
                .collect(Collectors.toMap(
                    d -> d.id(),
                    d -> Optional.ofNullable(d.date())
                ));
    }


            
    @Transactional
    public Map<Identity, Optional<Date>> getLastMatchDatesForLeagues(
            List<Identity> leagueIds) {
    
        List<MatchRepository.DateForId> lastMatchDateByLeagueIds = 
            Collections.emptyList();
        if (!leagueIds.isEmpty()) {
            lastMatchDateByLeagueIds.addAll(matchRepository
                    .findLastMatchDateByLeagueIds(leagueIds));
        }
        
        return lastMatchDateByLeagueIds
                .stream()
                .collect(Collectors.toMap(
                    d -> d.id(),
                    d -> Optional.ofNullable(d.date())
                ));
    }

    @Transactional
    public Map<Identity, Optional<Date>> getLastMatchDatesForTeams(List<Team> teams) {
        Map<Identity, Optional<Date>> lastMatchDatesByTeamId = new HashMap<>();
        teams.forEach(team ->
             lastMatchDatesByTeamId.put(team.getId(), matchRepository
                .findTopByTeamsContainsOrderByStartedDesc(team).map(Match::getStarted)));
        return lastMatchDatesByTeamId;
    }

    @Transactional
    public List<Match> createOrUpdateMatches(MatchesResponse matchesResponse, int opus) {
        if (matchesResponse == null || matchesResponse.isEmpty()) {
            return Collections.emptyList();
        }

        List<Match> matches = Arrays
                .stream(matchesResponse.getMatches())
                .map((apiMatch) -> internalCreateOrUpdateMatch(apiMatch, opus))
                .toList();
        return matchRepository.saveAll(matches);
    }

    @Transactional
    public Match createOrUpdateMatch(MatchResponse matchResponse, int opus) {
        if (matchResponse == null || matchResponse.isEmpty()) {
            return null;
        }
        
        Optional<ApiMatch> apiMatch = ofNullable(matchResponse.getMatch());
        Optional<Match> match = apiMatch.map((x) -> internalCreateOrUpdateMatch(x, opus));
        return match.map(matchRepository::save).orElse(null);
    }

    private Match internalCreateOrUpdateMatch(ApiMatch apiMatch, int opus) {        
        Match match = newMatchOrFromDb(
            new SimpleIdentity(apiMatch.getId(), opus));
        populateMatch(apiMatch, opus, match);
        return match;
    }

    private Match newMatchOrFromDb(Identity id) {
        Optional<Match> matchFromDb = matchRepository.findById(id);
        if (matchFromDb.isPresent()) {
            return matchFromDb.get();
        }
        return new Match(id);
    }

    public void populateMatch(ApiMatch sourceApiMatch, int opus, Match targetMatch) {
        PopulatorUtil.copyNonNullProperties(sourceApiMatch, targetMatch);
/*
        targetMatch.setCoaches(
                Arrays.stream(sourceApiMatch.getCoaches())
                        .map((apiCoach) -> toCoach(apiCoach))
                        .collect(Collectors.toList()));
        targetMatch.setTeams(
                Arrays.stream(sourceApiMatch.getTeams())
                        .map((apiTeam) -> toTeam(apiTeam, opus))
                        .collect(Collectors.toList()));
*/
    }
/*
    private Match.Coach toCoach(ApiCoach apiCoach) {
        Match.Coach coach = new Match.Coach();
        PopulatorUtil.copyNonNullProperties(apiCoach, coach);
        return coach;
    }

    private Team toTeam(ApiTeam apiTeam, int opus) {
        Team team = new Team(new SimpleIdentity(apiTeam.getId(), opus));
        teamPopulator.populateMatchTeam(apiTeam, team, opus);
        return team;
    }
*/
}
