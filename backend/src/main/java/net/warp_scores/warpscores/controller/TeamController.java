package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.model.Team;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamDomainService teamDomainService;

    @GetMapping("/league/{leagueId}/teams")
    public ResponseEntity<List<Team>> getTeamsForLeague(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Team> teams = teamDomainService.findByLeagueId(leagueId);
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for league {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<Team> getTeam(@PathVariable(name = "teamId") UUID teamId) {
        try {
            Optional<Team> team = teamDomainService.findTeam(teamId, Optional.empty());
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for {}", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionId}/teams")
    public ResponseEntity<List<Team>> getTeamsForCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            List<Team> teams = teamDomainService.findByCompetitionId(competitionId);
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition {}", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionUuid}/team/{teamUuid}")
    public ResponseEntity<Team> getTeam(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "teamUuid") UUID teamUuid) {
        try {
            Optional<Team> team = teamDomainService.findTeam(teamUuid, Optional.of(competitionUuid));
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for competition {} (teamId: {})", competitionUuid, teamUuid, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
