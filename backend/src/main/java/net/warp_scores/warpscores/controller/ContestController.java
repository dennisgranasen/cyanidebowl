package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.model.Contest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContestController {

    private final ContestRepository contestRepository;

    @GetMapping("/contests/competition/{competitionUuid}")
    public ResponseEntity<List<Contest>> getCompetitionContests(@PathVariable(name = "competitionUuid") UUID competitionUuid) {
        try {
            List<Contest> contests = contestRepository.findByCompetitionId(competitionUuid);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
