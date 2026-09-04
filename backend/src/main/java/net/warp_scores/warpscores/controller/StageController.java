package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.StageMatchService;
import net.warp_scores.warpscores.service.StageNotFoundException;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StageController {

    private final StageMatchService stageMatchService;
    private final ReplayDownloadRepository replayDownloads;

    @GetMapping("/stages/{stageId}/matches")
    public ResponseEntity<List<StageMatchResponse>> getStageMatches(
            @PathVariable String stageId) {
        try {
            var matches = stageMatchService.getMatchesForStage(stageId);
            var ids = matches.stream().filter(match -> match.sourceMatchId() != null)
                    .map(match -> match.sourceMatchId().asMongoKey()).toList();
            var replayIds = replayDownloads.findAllById(ids).stream()
                    .filter(replay -> "DOWNLOADED".equals(replay.getStatus()))
                    .map(replay -> replay.getMatchId()).collect(java.util.stream.Collectors.toSet());
            return ResponseEntity.ok(matches.stream()
                    .map(match -> StageMatchResponse.from(match, match.sourceMatchId() != null
                            && replayIds.contains(match.sourceMatchId().asMongoKey())))
                    .toList());
        } catch (StageNotFoundException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
