package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplayAnalysisBackfillService {
    private final ReplayDownloadRepository downloads;
    private final ReplayArtifactService artifacts;
    @Value("${replay-analysis.enabled:true}") private boolean enabled;

    @Scheduled(fixedDelayString="${replay-analysis.fixed-delay-ms:600000}",
            initialDelayString="${replay-analysis.initial-delay-ms:120000}")
    public void analyzeNewestPending() {
        if (!enabled) return;
        downloads.findPendingAnalysis(ReplayArtifactService.PARSER_VERSION, PageRequest.of(0, 1)).stream().findFirst()
                .ifPresent(record -> {
                    try {
                        artifacts.reanalyze(record);
                    } catch (Exception error) {
                        record.setAnalysisStatus("FAILED");
                        record.setParserVersion(ReplayArtifactService.PARSER_VERSION);
                        record.setAnalyzedAt(new Date());
                        record.setAnalysisError(Objects.toString(error.getMessage(), "Replay analysis failed"));
                        downloads.save(record);
                        log.warn("Unable to analyze replay {}", record.getMatchId(), error);
                    }
                });
    }

    public void analyze(String matchId) {
        var record = downloads.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("No downloaded replay exists for match " + matchId));
        try {
            artifacts.reanalyze(record);
        } catch (Exception error) {
            record.setAnalysisStatus("FAILED");
            record.setParserVersion(ReplayArtifactService.PARSER_VERSION);
            record.setAnalyzedAt(new Date());
            record.setAnalysisError(Objects.toString(error.getMessage(), "Replay analysis failed"));
            downloads.save(record);
            throw new IllegalStateException("Replay analysis failed", error);
        }
    }
}
