package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.model.Competition;
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

    @GetMapping("/competitions/league/{leagueId}/{status}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeagueAndStatus(@PathVariable(name = "leagueId") UUID leagueId,
            @PathVariable(name = "status")
            CompetitionStatus status) {
        try {
            List<Competition> competitions = competitionService.loadForLeagueAndStatus(leagueId, status);
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {} and status {}", leagueId, status, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeague(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Competition> competitions = competitionService.loadForLeague(leagueId);
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {}", leagueId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionId}")
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
}
