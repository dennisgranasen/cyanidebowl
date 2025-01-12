package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.CompetitionTeamsDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
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

    private final MatchDomainService matchDomainService;

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<Team> getTeam(@PathVariable(name = "teamId") UUID teamId) {
        try {
            Optional<Team> team = teamDomainService.findTeam(teamId, Optional.empty());
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for uuid {}.", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/{teamId}/matches")
    public ResponseEntity<List<Match>> getMatches(@PathVariable(name = "teamId") UUID teamId) {
        try {
            Optional<List<Match>> matchesForTeam = Optional.ofNullable(matchDomainService.findMatchesForTeam(teamId));
            return matchesForTeam
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get matches for team uuid {}.", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
