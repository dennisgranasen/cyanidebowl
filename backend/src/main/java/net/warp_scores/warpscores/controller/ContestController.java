package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.service.ContestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @GetMapping("/contests/competition/{competitionUuid}")
    public ResponseEntity<List<Contest>> getCompetitionContests(@PathVariable(name = "competitionUuid") UUID competitionUuid) {
        try {
            List<Contest> contests = contestService.getCompetitionContests(competitionUuid);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/league/{leagueUuid}/latest")
    public ResponseEntity<List<Contest>> getLatestLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        try {
            List<Contest> contests = contestService.getLatestLeagueContests(leagueUuid);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/league/{leagueUuid}/live")
    public ResponseEntity<List<Contest>> getLiveLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        try {
            List<Contest> contests = contestService.getLiveLeagueContests(leagueUuid);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
