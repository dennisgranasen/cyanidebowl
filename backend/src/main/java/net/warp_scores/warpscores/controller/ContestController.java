package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.service.ContestService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
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

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @GetMapping("/contests/competition/{competitionId}/latest")
    public ResponseEntity<List<Contest>> getLatestCompetitionContests(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus,
            @RequestParam(name = "limit", required = false) Integer limit) {
        try {
            limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LIVE_CONTESTS);

            String[] parts = competitionId.split(Identity.DELIMITER);
            Identity competitionIdentity = 
                new CompositeIdentity(ofNullable(opus).orElse(defaultOpus), parts);

            List<Contest> contests = contestService.getLatestCompetitionContests(
                competitionIdentity, limit);
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
            log.warn("Using endpoint /contests/competition/{competitionId} ", competitionId);
            String[] parts = competitionId.split(Identity.DELIMITER);
            Identity competitionIdentity = 
                new CompositeIdentity(ofNullable(opus).orElse(defaultOpus), parts);         
            log.info("Retrieving contests for competition: {}", competitionIdentity);
            List<Contest> contests = contestService.getCompetitionContests(
                competitionIdentity,
                Optional.ofNullable(limit));
            log.info("Retrieved {} contests for competition: {}", contests.size(), competitionIdentity);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/contests/competition/{competitionId}")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Void> addContest(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus,
            @RequestBody Contest contest) {
        try {
            // todo: what is competitionId for?
            contestDomainService.addContest(contest, Optional.ofNullable(opus).orElse(defaultOpus));
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
            Identity leagueIdentity = 
                new SimpleIdentity(leagueId, ofNullable(opus).orElse(defaultOpus));

            List<Contest> contests = contestService.getLatestLeagueContests(
                leagueIdentity, limit);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests for " + leagueId + " with opus " + opus, ex);
            return ResponseEntity.internalServerError().build();
        }
    }    

    @GetMapping("/contests/league/{leagueId}/live")
    public ResponseEntity<List<Contest>> getLiveLeagueContests(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "opus", required = false) Integer opus, 
            @RequestParam(name = "limit", required = false) Integer limit
            ) {
        try {
            Integer contestLimit = ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LIVE_CONTESTS);
            List<Contest> contests = contestService.getLiveLeagueContests(
                new SimpleIdentity(leagueId, ofNullable(opus).orElse(defaultOpus)),
                contestLimit);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
