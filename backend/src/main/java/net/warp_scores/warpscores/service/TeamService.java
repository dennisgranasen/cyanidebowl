package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.In;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final CyanideApiService cyanideApiService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Value("${cyanide.defaults.pageLimit:100}")
    private int defaultPageLimit;

    @DurationLogging
    public Optional<Team> loadById(Identity teamId) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isPresent()) {
            log.info("Loaded team by ID: {}", teamId);
            return team;
        } else {
            log.warn("Team with ID {} not found in repository. Loading from cyanide", teamId);
            team = cyanideApiService.loadTeam(teamId, true, ofNullable(true));
            if (team.isEmpty()) {
                log.warn("Team with ID {} not found in Cyanide API", teamId);
                return Optional.empty();
            }
            log.info("Team {} loaded from Cyanide API, saving to repository", teamId);
            teamRepository.save(team.get());
            return team;
        }
    }

    @DurationLogging
    public List<Team> getTeamsForLeague(Identity leagueId) {
        List<Team> teams = teamRepository.findByLeagueId(leagueId);
        if (teams.isEmpty()) {
            log.warn("No teams found for league ID {}", leagueId);
            return List.of();
        }
        log.info("Found {} teams for league ID {}", teams.size(), leagueId);
        return teams;
    } 

    @DurationLogging
    public List<Team> getTeamsForCompetition(Identity competitionId) { 
        List<Team> teams = teamRepository.findByCompetitionId(competitionId);
        if (teams.isEmpty()) {
            log.warn("No teams found for competition ID {}", competitionId);
            return List.of();
        }
        log.info("Found {} teams for competition ID {}", teams.size(), competitionId);
        return teams;
    }

    @DurationLogging
    public List<Team> loadAll() {
        return teamRepository.findAll();
    }

}