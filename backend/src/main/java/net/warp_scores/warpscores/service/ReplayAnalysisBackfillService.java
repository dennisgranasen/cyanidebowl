package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplayAnalysisBackfillService {
    private final ReplayDownloadRepository downloads;
    private final ReplayArtifactService artifacts;
    @Value("${replay-analysis.enabled:true}") private boolean enabled;
    @Value("${replay-analysis.fixed-delay-ms:1000}") private long fixedDelayMs;
    private final AtomicReference<String> runningMatchId = new AtomicReference<>();
    private volatile Instant lastStartedAt;
    private volatile Instant lastCompletedAt;

    @Scheduled(scheduler="replayAnalysisScheduler", fixedDelayString="${replay-analysis.fixed-delay-ms:1000}",
            initialDelayString="${replay-analysis.initial-delay-ms:10000}")
    public void analyzeNewestPending() {
        if (!enabled) return;
        nextAvailable()
                .ifPresent(record -> {
                    if (!runningMatchId.compareAndSet(null, record.getMatchId())) return;
                    lastStartedAt = Instant.now();
                    try {
                        artifacts.reanalyze(record);
                    } catch (Exception error) {
                        record.setAnalysisStatus("FAILED");
                        record.setAnalysisAttemptVersion(ReplayArtifactService.PARSER_VERSION);
                        record.setAnalyzedAt(new Date());
                        record.setAnalysisRequestedAt(null);
                        record.setAnalysisRequestedBy(null);
                        record.setAnalysisError(Objects.toString(error.getMessage(), "Replay analysis failed"));
                        downloads.save(record);
                        log.warn("Unable to analyze replay {}", record.getMatchId(), error);
                    } finally {
                        lastCompletedAt = Instant.now();
                        runningMatchId.compareAndSet(record.getMatchId(), null);
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
        requestReanalysis(matchId, "site-admin");
    }

    public void requestReanalysis(String matchId, String requestedBy) {
        var record = downloads.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("No downloaded replay exists for match " + matchId));
        if (!artifacts.originalAvailable(record)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Replay file is missing from local storage. Restore the file before analyzing again.");
        }
        record.setAnalysisRequestedAt(new Date());
        record.setAnalysisRequestedBy(requestedBy == null || requestedBy.isBlank() ? "site-admin" : requestedBy);
        record.setAnalysisError(null);
        downloads.save(record);
    }

    public QueueSnapshot snapshot() {
        Instant now = Instant.now();
        List<QueueJob> jobs = new ArrayList<>();
        long total = 0;
        long runnable = 0;
        long blocked = 0;
        String running = runningMatchId.get();
        for (int page = 0; ; page++) {
            var records = downloads.findPendingAnalysis(
                    ReplayArtifactService.PARSER_VERSION,
                    PageRequest.of(page, 100));
            for (var record : records) {
                if (Objects.equals(record.getMatchId(), running)) continue;
                total++;
                boolean available = artifacts.originalAvailable(record);
                if (available) runnable++; else blocked++;
                if (jobs.size() < 200) jobs.add(toQueueJob(record, available));
            }
            if (records.size() < 100) break;
        }
        jobs.sort(Comparator
                .comparingInt((QueueJob job) -> reasonRank(job.reason()))
                .thenComparing(QueueJob::requestedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(QueueJob::downloadedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Date hourCutoff = Date.from(now.minus(Duration.ofHours(1)));
        Date dayCutoff = Date.from(now.minus(Duration.ofHours(24)));
        long completedHour = 0;
        long completedDay = 0;
        List<Failure> failures = new ArrayList<>();
        for (var record : downloads.findAll()) {
            Date analyzed = record.getAnalyzedAt();
            if (analyzed != null && !analyzed.before(dayCutoff)) {
                completedDay++;
                if (!analyzed.before(hourCutoff)) completedHour++;
            }
            if ("FAILED".equals(record.getAnalysisStatus()) && record.getAnalysisError() != null) {
                failures.add(new Failure(record.getMatchId(), record.getParserVersion(),
                        record.getAnalysisAttemptVersion(), record.getAnalyzedAt(), record.getAnalysisError()));
            }
        }
        failures.sort(Comparator.comparing(Failure::analyzedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (failures.size() > 10) failures = new ArrayList<>(failures.subList(0, 10));

        Long eta = estimateSeconds(total, completedHour, completedDay);
        Instant nextCheck = enabled && running == null && total > 0
                ? (lastCompletedAt == null ? now : max(now, lastCompletedAt.plusMillis(Math.max(1, fixedDelayMs))))
                : null;
        return new QueueSnapshot(enabled, ReplayArtifactService.PARSER_VERSION, total, runnable, blocked,
                running, lastStartedAt, lastCompletedAt, nextCheck, completedHour, completedDay, eta, jobs, failures);
    }

    private QueueJob toQueueJob(net.warp_scores.warpscores.model.ReplayDownload record, boolean available) {
        return new QueueJob(record.getMatchId(), record.getGameId(), reason(record),
                available ? "QUEUED" : "BLOCKED", record.getParserVersion(), record.getAnalysisAttemptVersion(),
                ReplayArtifactService.PARSER_VERSION, available, record.getDownloadedAt(), record.getAnalyzedAt(),
                record.getAnalysisRequestedAt(), record.getAnalysisRequestedBy(), record.getAnalysisError());
    }

    private QueueReason reason(net.warp_scores.warpscores.model.ReplayDownload record) {
        if (record.getAnalysisRequestedAt() != null) return QueueReason.MANUAL_REANALYSIS;
        if (record.getAnalyzedAt() == null || record.getAnalysisStatus() == null || "PENDING".equals(record.getAnalysisStatus()))
            return QueueReason.NEW_REPLAY;
        return QueueReason.PARSER_VERSION_OUTDATED;
    }

    private static int reasonRank(QueueReason reason) {
        return switch (reason) {
            case MANUAL_REANALYSIS -> 0;
            case NEW_REPLAY -> 1;
            case PARSER_VERSION_OUTDATED -> 2;
        };
    }

    private static Long estimateSeconds(long jobs, long completedHour, long completedDay) {
        if (jobs == 0) return 0L;
        double perHour = completedHour > 0 ? completedHour : completedDay / 24.0;
        return perHour <= 0 ? null : (long) Math.ceil(jobs / perHour * 3600.0);
    }

    private static Instant max(Instant left, Instant right) {
        return left.isAfter(right) ? left : right;
    }

    public enum QueueReason { MANUAL_REANALYSIS, NEW_REPLAY, PARSER_VERSION_OUTDATED }
    public record QueueJob(String matchId, String gameId, QueueReason reason, String status,
                           Integer parserVersion, Integer analysisAttemptVersion, int targetParserVersion,
                           boolean originalAvailable, Date downloadedAt, Date analyzedAt,
                           Date requestedAt, String requestedBy, String error) {}
    public record Failure(String matchId, Integer parserVersion, Integer analysisAttemptVersion,
                          Date analyzedAt, String error) {}
    public record QueueSnapshot(boolean enabled, int parserVersion, long queued, long runnable, long blocked,
                                String runningMatchId, Instant lastStartedAt, Instant lastCompletedAt,
                                Instant nextWorkerCheckAt, long completedLastHour, long completedLast24Hours,
                                Long estimatedClearSeconds, List<QueueJob> jobs, List<Failure> recentFailures) {}
}
