package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.model.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;

    private final CompetitionRepository competitionRepository;

    @GetMapping("/teams/league/{leagueId}")
    public ResponseEntity<List<Team>> getTeamsForLeague(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Team> teams = teamRepository.findByLeagueId(leagueId);
            removeInactiveCompetitionsFromTeams(teams);

            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for league {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/competition/{competitionId}")
    public ResponseEntity<List<Team>> getTeamsForCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            List<Team> teams = teamRepository.findByCompetitionId(competitionId);
            removeInactiveCompetitionsFromTeams(teams);
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition {}", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/team/{teamUuid}")
    public ResponseEntity<Team> getTeam(@PathVariable(name = "teamUuid") UUID teamUuid) {
        List<Team> teams = teamRepository.findAllById(Arrays.asList(teamUuid));
        if (teams.size() == 1) {
            removeInactiveCompetitionsFromTeams(teams);
            return ResponseEntity.ok(teams.get(0));
        } else {
            log.error("Unable to find team with uuid {}", teamUuid);
            return ResponseEntity.notFound().build();
        }
    }

    private void removeInactiveCompetitionsFromTeams(List<Team> teams) {
        Set<UUID> competitionIds = teams
                .stream()
                .map(Team::getCompetitionIds)
                .flatMap(array -> Arrays.stream(array))
                .collect(Collectors.toSet());
        List<Competition> competitions = competitionRepository.findAllById(competitionIds);
        Set<UUID> inactiveCompetitionUuids = competitions
                .stream()
                .filter(comp -> !CompetitionStatus.InProgress.equals(comp.getStatus()))
                .map(Competition::getUuid)
                .collect(Collectors.toSet());
        teams
                .stream()
                .forEach(team -> removeInactiveCompetitionsFromTeam(team, inactiveCompetitionUuids));
    }

    private void removeInactiveCompetitionsFromTeam(Team team, Set<UUID> inactiveCompetitionUuids) {
        Set<UUID> competitionIds = Arrays.stream(team.getCompetitionIds()).collect(Collectors.toSet());
        competitionIds.removeAll(inactiveCompetitionUuids);
        team.setCompetitionIds(competitionIds.toArray(new UUID[0]));
    }
}
