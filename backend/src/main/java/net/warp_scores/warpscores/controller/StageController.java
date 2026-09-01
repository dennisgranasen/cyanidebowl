package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.StageMatchService;
import net.warp_scores.warpscores.service.StageNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StageController {

    private final StageMatchService stageMatchService;

    @GetMapping("/stages/{stageId}/matches")
    public ResponseEntity<List<StageMatchResponse>> getStageMatches(
            @PathVariable String stageId) {
        try {
            return ResponseEntity.ok(stageMatchService.getMatchesForStage(stageId).stream()
                    .map(StageMatchResponse::from)
                    .toList());
        } catch (StageNotFoundException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
