package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.MatchRepository.TeamRankingRecord;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Rank;
import net.warp_scores.warpscores.service.RankService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RankController {

    private final RankService rankService;
    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @GetMapping("/ranks/competition/{competitionId}")
    public ResponseEntity<List<Rank>> getRanksForCompetition(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "opus", required = false) Integer opus) {
        log.info("Fetching ranks for competition with ID: {} and opus: {}", competitionId, opus);
        if (competitionId == null) {
            log.error("competitionId is null");
            return ResponseEntity.badRequest().build();
        }
        try {
            String[] parts = competitionId.split(Identity.DELIMITER);
            Identity competitionIdentity = 
                new CompositeIdentity(ofNullable(opus).orElse(defaultOpus), parts);

            List<Rank> ranks = rankService.getRanksForCompetition(
                competitionIdentity,
                Optional.empty(), Optional.ofNullable(limit));
            if (ranks == null || ranks.isEmpty()) {
                log.warn("No ranks found for competition with ID: {}", competitionId);
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(ranks);
        } catch (NoSuchElementException ex) {
            log.error("Caught NoSuchElementException.", ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Caught Exception.", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/ranks/league/{leagueId}")
    public ResponseEntity<List<TeamRankingRecord>> getRanksForLeague(
            @PathVariable(name = "leagueId") String leagueId,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "opus", required = false) Integer opus) {
        log.info("Fetching ranks for league with ID: {} and opus: {}", leagueId, opus);
        if (leagueId == null) {
            log.error("leagueId is null");
            return ResponseEntity.badRequest().build();
        }
        try {
            List<TeamRankingRecord> ranks = rankService.getRanksForLeague(
                new SimpleIdentity(leagueId, Optional.ofNullable(opus).orElse(defaultOpus)),
                Optional.empty(), Optional.ofNullable(limit));
            if (ranks == null || ranks.isEmpty()) {
                log.warn("No ranks found for league with ID: {}", leagueId);
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(ranks);
        } catch (NoSuchElementException ex) {
            log.error("Caught NoSuchElementException.", ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Caught Exception.", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

}

