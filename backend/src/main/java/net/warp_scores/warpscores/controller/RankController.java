package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Rank;
import net.warp_scores.warpscores.service.RankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/ranks/competition/{competitionId}")
    public ResponseEntity<List<Rank>> getRanksForCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        if (competitionId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            List<Rank> ranks = rankService.getRanksForCompetition(competitionId, Optional.empty());
            if (ranks == null || ranks.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ranks);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

