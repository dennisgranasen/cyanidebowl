package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.service.ContestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.warp_scores.warpscores.controller.Authorizations.WRITE_LEAGUE_ADMIN;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ContestController {

    public static final int MAX_LIMIT_FOR_LATEST_CONTESTS = 12;
    public static final int DEFAULT_LIMIT_FOR_LATEST_CONTESTS = 6;
    private final ContestService contestService;
    private final ContestDomainService contestDomainService;

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

    @PostMapping("/contests/competition/{competitionUuid}")
    @PreAuthorize(WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Void> addContest(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @RequestBody Contest contest) {
        try {
            contestDomainService.addContest(contest);
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            log.error("Unable to add contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/league/{leagueUuid}/latest")
    public ResponseEntity<List<Contest>> getLatestLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        return getLatestLeagueContests(leagueUuid, null);
    }

    @GetMapping("/contests/league/{leagueUuid}/latest/{limit}")
    public ResponseEntity<List<Contest>> getLatestLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid,
            @PathVariable(name = "limit") Integer limit) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_CONTESTS);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_CONTESTS);
        try {
            List<Contest> contests = contestService.getLatestLeagueContests(leagueUuid, limit);
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
