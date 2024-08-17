package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.Rank;
import net.warp_scores.warpscores.service.RankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RankController {

    private final RankService rankService;

    @GetMapping("/ranks/competition/{competitionId}")
    public ResponseEntity<List<Rank>> getRanksForCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        if (competitionId == null) {
            log.error("competitionId is null");
            return ResponseEntity.badRequest().build();
        }
        try {
            List<Rank> ranks = rankService.getRanksForCompetition(competitionId, Optional.empty());
            if (ranks == null || ranks.isEmpty()) {
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

