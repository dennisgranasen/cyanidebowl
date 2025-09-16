package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;
import net.warp_scores.warpscores.service.TeamService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamDomainService teamDomainService;
    private final TeamService teamService;

    private final MatchDomainService matchDomainService;

    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

    @GetMapping("/team/{teamId}")
    public ResponseEntity<Team> getTeam(
            @PathVariable(name = "teamId") String teamId) {
        try {
            Identity teamIdentity = IdentityUtil.fromId(teamId);
            Optional<Team> team = teamDomainService.findTeam(teamIdentity);
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception ex) {
            log.error("Unable to get team for id {}.", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/{teamIds}")
    public ResponseEntity<List<Team>> getTeams(
            @PathVariable(name = "teamIds") String teamIds) {
        String[] teamIdArray = teamIds.split(",");
        if (teamIdArray.length == 0) {
            return ResponseEntity.badRequest().build();
        }
        List<Identity> ids = Arrays.stream(teamIdArray)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(IdentityUtil::fromId)
                .toList();
        try {
            List<Team> teams = teamDomainService.findTeams(ids);
            if (teams.isEmpty()) {
                log.warn("No teams found for ids: {}", teamIds);
                return ResponseEntity.noContent().build();
            }
            // Adjust competition names for each team
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get team for id {}.", teamIds, ex);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/teams/{teamId}/matches")
    public ResponseEntity<List<Match>> getMatches(@PathVariable(name = "teamId") String teamId) {
        try {
            Identity teamIdentity = IdentityUtil.fromId(teamId);
            List<Match> matchesForTeam = 
                matchDomainService.findMatchesForTeam(teamIdentity);
            matchesForTeam.forEach(match -> officialLeagueAndCompetitions.adjustCompetitionName(match.getLeagueId(),
                    match.getCompetitionName(), match::setCompetitionName));
            return ResponseEntity.ok(matchesForTeam);
        } catch (Exception ex) {
            log.error("Unable to get matches for team id {}.", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/league/{leagueId}")
    public ResponseEntity<List<Team>> getTeamsForLeague(@PathVariable(name = "leagueId") String leagueId) {
        try {
            Identity leagueIdentity = IdentityUtil.fromId(leagueId);
            List<Team> teamsForLeague = teamService.getTeamsForLeague(leagueIdentity);
            return ResponseEntity.ok(teamsForLeague);
        } catch (Exception ex) {
            log.error("Unable to get teams for league id {}.", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/competition/{competitionId}")
    public ResponseEntity<List<Team>> getTeamsForCompetition(
            @PathVariable(name = "competitionId") String competitionId) {
        try {
            Identity competitionIdentity = IdentityUtil.fromId(competitionId);
            List<Team> teamsForCompetition = teamService.getTeamsForCompetition(competitionIdentity);
            return ResponseEntity.ok(teamsForCompetition);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition id {}.", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/circuit/{circuitId}")
    public ResponseEntity<List<Team>> getTeamsForCircuit(
            @PathVariable(name = "circuitId") Long circuitId) {
        try {
            List<Team> teamsForCircuit = teamService.getTeamsForCircuit(circuitId);
            return ResponseEntity.ok(teamsForCircuit);
        } catch (Exception ex) {
            log.error("Unable to get teams for circuit id {}.", circuitId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/teams/circuit/{circuitId}/leg/{circuitLegId}")
    public ResponseEntity<List<Team>> getTeamsForCircuitLeg(
            @PathVariable(name = "circuitId") Long circuitId,
            @PathVariable(name = "circuitLegId") Long circuitLegId) {
        try {
            List<Team> teamsForCircuit = teamService.getTeamsForCircuitLeg(circuitId, circuitLegId);
            return ResponseEntity.ok(teamsForCircuit);
        } catch (Exception ex) {
            log.error("Unable to get teams for circuit id {} and leg id {}.", circuitId, circuitLegId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/circuit/{circuitId}/leg/{circuitLegId}/{entityId}")
    public ResponseEntity<List<Team>> getTeamsForCircuitLegEntity(
            @PathVariable(name = "circuitId") Long circuitId,
            @PathVariable(name = "circuitLegId") Long circuitLegId,
            @PathVariable(name = "entityId") String entityId) {
        try {
            Identity eid = IdentityUtil.fromId(entityId);

            List<Team> teamsForCircuit = teamService.getTeamsForCircuitLegEntity(circuitId, circuitLegId, eid);
            return ResponseEntity.ok(teamsForCircuit);
        } catch (Exception ex) {
            log.error("Unable to get teams for circuit id {} and leg id {} and entity id {}.", circuitId, circuitLegId, entityId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
