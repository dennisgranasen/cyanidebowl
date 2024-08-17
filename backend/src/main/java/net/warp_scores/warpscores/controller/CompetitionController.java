package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    private final TeamDomainService teamDomainService;

    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getActiveCompetitionsForLeague(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Competition> competitions = competitionService.loadForLeague(leagueId);
            competitions = competitions
                    .stream()
                    .filter(competitionService::competitionConsideredActive)
                    .sorted()
                    .collect(Collectors.toUnmodifiableList());
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/{competitionId}")
    public ResponseEntity<Competition> getCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            Optional<Competition> competition = competitionService.loadCompetition(competitionId);
            return competition
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get competition {}", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/{competitionId}/teams")
    public ResponseEntity<List<Team>> getTeamsForCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            List<Team> teams = teamDomainService.findByCompetitionId(competitionId);
            return ResponseEntity.ok(teams);
        } catch (Exception ex) {
            log.error("Unable to get teams for competition uuid {}.", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/{competitionUuid}/team/{teamUuid}")
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
