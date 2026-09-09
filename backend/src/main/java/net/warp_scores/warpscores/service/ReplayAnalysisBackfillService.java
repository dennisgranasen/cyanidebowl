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
        nextAvailable()
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

    private java.util.Optional<net.warp_scores.warpscores.model.ReplayDownload> nextAvailable() {
        for (int page = 0; ; page++) {
            var records = downloads.findPendingAnalysis(ReplayArtifactService.PARSER_VERSION, PageRequest.of(page, 50));
            var available = records.stream().filter(artifacts::originalAvailable).findFirst();
            if (available.isPresent() || records.size() < 50) return available;
        }
    }

    public void analyze(String matchId) {
        var record = downloads.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("No downloaded replay exists for match " + matchId));
        if (!artifacts.originalAvailable(record)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Replay file is missing from local storage. Restore the file before analyzing again.");
        }
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
