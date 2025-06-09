package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Coach;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ContestService;

import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_LEAGUE_ADMIN;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ContestController {

    public static final int MAX_LIMIT_FOR_LATEST_CONTESTS = 24;
    public static final int DEFAULT_LIMIT_FOR_LATEST_CONTESTS = 6;
    public static final int DEFAULT_LIMIT_FOR_LIVE_CONTESTS = 15;
    public static final int DEFAULT_LIMIT_FOR_CONTESTS = 100;
    private final ContestService contestService;
    private final ContestDomainService contestDomainService;


    @GetMapping("/contests/competition/{competitionId}/latest")
    public ResponseEntity<List<Contest>> getLatestCompetitionContests(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus,
            @RequestParam(name = "limit", required = false) Integer limit) {
        try {
            limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LIVE_CONTESTS);
            List<Contest> contests = contestService.getLatestCompetitionContests(
                competitionId, Optional.ofNullable(opus), limit);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/competition/{competitionId}")
    public ResponseEntity<List<Contest>> getCompetitionContests(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Contest> contests = contestService.getCompetitionContests(
                competitionId,
                Optional.ofNullable(opus),
                Optional.ofNullable(limit));
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/contests/competition/{competitionId}")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Void> addContest(
            @PathVariable(name = "competitionId") String competitionUuid,
            @RequestBody Contest contest) {
        try {
            contestDomainService.addContest(contest);
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            log.error("Unable to add contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/league/{leagueId}/latest")
    public ResponseEntity<List<Contest>> getLatestLeagueContests(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus,
            @RequestParam(name = "limit", required = false) Integer limit) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_CONTESTS);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_CONTESTS);
        try {
            List<Contest> contests = contestService.getLatestLeagueContests(
                leagueId, Optional.ofNullable(opus), limit);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
