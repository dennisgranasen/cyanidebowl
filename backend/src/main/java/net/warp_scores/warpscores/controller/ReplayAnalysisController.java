package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import net.warp_scores.warpscores.service.ReplayStatisticsService;
import net.warp_scores.warpscores.service.ReplayArtifactService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReplayAnalysisController {
    private final ReplayAnalysisRepository analyses;
    private final ReplayDownloadRepository downloads;
    private final ReplayStatisticsService statistics;
    private final ReplayArtifactService artifacts;

    @GetMapping("/matches/{matchId}/replay")
    public Map<String, Object> replay(@PathVariable String matchId) {
        return downloads.findById(matchId).map(download -> {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("available", "DOWNLOADED".equals(download.getStatus()));
            result.put("status", download.getStatus());
            result.put("analysisStatus", download.getAnalysisStatus());
            result.put("downloadedAt", download.getDownloadedAt());
            result.put("originalSize", download.getOriginalSize());
            result.put("compactSize", download.getCompactSize());
            result.put("parserVersion", download.getParserVersion());
            result.put("analysisError", download.getAnalysisError());
            result.put("analysis", analyses.findById(matchId).orElse(null));
            return result;
        }).orElseGet(() -> Map.of("available", false, "status", "NOT_DOWNLOADED"));
    }

    @GetMapping("/matches/{matchId}/replay/original")
    public ResponseEntity<byte[]> original(@PathVariable String matchId) {
        try {
            var replay = artifacts.readOriginal(matchId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + replay.fileName() + "\"")
                    .contentType(MediaType.parseMediaType(replay.fileName().endsWith(".gz") ? "application/gzip" : "application/xml"))
                    .body(replay.data());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/{matchId}/replay-analysis")
    public ResponseEntity<ReplayAnalysis> match(@PathVariable String matchId) {
        return analyses.findById(matchId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/replay-statistics")
    public Map<String, Object> totals(@RequestParam(required=false) String coachId,
                                      @RequestParam(required=false) String teamId) {
        return statistics.totals(coachId, teamId);
    }
}
