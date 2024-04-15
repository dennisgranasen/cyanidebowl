package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.cyanide.api.requests.CompetitionsRequest;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeagueAndStatus(@PathVariable(name = "leagueId") UUID leagueId,
            @RequestBody CompetitionStatus... competitionStatuses) {
        try {
            List<Competition> competitions = competitionService.loadForLeagueAndStatuses(leagueId, competitionStatuses);
            competitions = competitions.stream().filter(competition -> competition.getPlayedMatches() > 0)
                    .collect(Collectors.toUnmodifiableList());
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            log.error("Unable to get competitions for league id {} and statuses {}", leagueId, competitionStatuses, ex);
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
