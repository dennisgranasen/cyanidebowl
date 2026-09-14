package net.warp_scores.warpscores.ai.reporting;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerRatingJobRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerRatingTaskRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.model.AiPlayerRatingJob;
import net.warp_scores.warpscores.model.AiPlayerRatingTask;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPlayerRatingJobService {
    private static final int WORKERS = 3;
    private static final int TEMPORARY_DEFAULT_REPORTER_LIMIT = 5;
    private static final int MAX_TASK_ATTEMPTS = 2;
    private static final long CONCURRENCY_RETRY_MS = 750L;

    private final AiReporterEffectiveProfileService effectiveProfiles;
    private final ReporterPlayerRatingService.PlayerRatingGenerator generator;
    private final AiPlayerMatchRatingRepository ratings;
    private final AiPlayerRatingJobRepository jobs;
    private final AiPlayerRatingTaskRepository tasks;
    private final ReplayAnalysisRepository replayAnalyses;
    private final PlayerRatingFactsBuilder factsBuilder;

    private final AtomicInteger workerNumber = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(
            WORKERS,
            runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "ai-player-rating-" + workerNumber.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });

    public JobSnapshot start(
            String matchId,
            Collection<String> reporterIds,
            String instruction,
            boolean force,
            int playerCount,
            String requestedBy) {
        List<AiReporterDefinition> selected = selectReporters(reporterIds);

        AiPlayerRatingJob job = new AiPlayerRatingJob();
        job.setId(UUID.randomUUID().toString());
        job.setMatchId(matchId);
        job.setRequestedBy(requestedBy);
        job.setInstruction(instruction);
        job.setForce(force);
        job.setPlayerCount(playerCount);
        job.setReporterCount(selected.size());
        job.setCreatedAt(Instant.now());
        jobs.save(job);

        List<AiPlayerRatingTask> pending = new ArrayList<>();
        for (AiReporterDefinition reporter : selected) {
            AiPlayerRatingTask task = new AiPlayerRatingTask();
            task.setId(job.getId() + ":" + reporter.getId());
            task.setJobId(job.getId());
            task.setMatchId(matchId);
            task.setReporterId(reporter.getId());
            task.setCreatedAt(Instant.now());

            boolean alreadyExists = !ratings.findByMatchIdAndReporterId(
                    matchId, reporter.getId()).isEmpty();
            if (!force && alreadyExists) {
                task.setStatus(AiPlayerRatingTask.Status.SKIPPED);
                task.setFinishedAt(Instant.now());
            } else {
                task.setStatus(AiPlayerRatingTask.Status.QUEUED);
            }
            pending.add(task);
        }
        tasks.saveAll(pending);

        pending.stream()
                .filter(task -> task.getStatus() == AiPlayerRatingTask.Status.QUEUED)
                .forEach(this::submit);

        return snapshot(job);
    }

    public Optional<JobSnapshot> cancel(String matchId, String jobId) {
        AiPlayerRatingJob job = jobs.findById(jobId).orElse(null);
        if (job == null || !matchId.equals(job.getMatchId())) return Optional.empty();

        Instant now = Instant.now();
        for (AiPlayerRatingTask task : tasks.findByJobIdOrderByReporterIdAsc(jobId)) {
            if (task.getStatus() == AiPlayerRatingTask.Status.QUEUED
                    || task.getStatus() == AiPlayerRatingTask.Status.RUNNING) {
                task.setStatus(AiPlayerRatingTask.Status.CANCELLED);
                task.setError("Cancelled by user");
                task.setFinishedAt(now);
                tasks.save(task);
            }
        }
        return Optional.of(snapshot(job));
    }

    public Optional<JobSnapshot> latestForMatch(String matchId) {
        return jobs.findFirstByMatchIdOrderByCreatedAtDesc(matchId).map(this::snapshot);
    }

    public Optional<JobSnapshot> byId(String jobId) {
        return jobs.findById(jobId).map(this::snapshot);
    }

    @PostConstruct
    void resumeInterruptedWork() {
        List<AiPlayerRatingTask> unfinished = tasks.findByStatusIn(
                List.of(
                        AiPlayerRatingTask.Status.QUEUED,
                        AiPlayerRatingTask.Status.RUNNING));
        for (AiPlayerRatingTask task : unfinished) {
            if (task.getStatus() == AiPlayerRatingTask.Status.RUNNING) {
                task.setStatus(AiPlayerRatingTask.Status.QUEUED);
                task.setStartedAt(null);
                tasks.save(task);
            }
            submit(task);
        }
    }

    @PreDestroy
    void stopWorkers() {
        executor.shutdownNow();
    }

    private void submit(AiPlayerRatingTask task) {
        executor.submit(() -> runTask(task.getId()));
    }

    private void runTask(String taskId) {
        AiPlayerRatingTask task = tasks.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != AiPlayerRatingTask.Status.QUEUED) {
            return;
        }

        task.setStatus(AiPlayerRatingTask.Status.RUNNING);
        task.setStartedAt(Instant.now());
        task.setError(null);
        tasks.save(task);

        try {
            AiPlayerRatingJob job = jobs.findById(task.getJobId())
                    .orElseThrow(() -> new IllegalStateException(
                            "AI player rating job no longer exists"));

            AiReporterDefinition reporter = effectiveProfiles.enabledForRatings().stream()
                    .map(AiReporterEffectiveProfileService.EffectiveReporter::definition)
                    .filter(definition -> definition.getId().equals(task.getReporterId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Reporter is no longer enabled for player ratings: "
                                    + task.getReporterId()));

            var analysis = replayAnalyses.findById(job.getMatchId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Analyzed replay disappeared for match " + job.getMatchId()));
            PlayerRatingFacts facts = factsBuilder.build(analysis);

            int attempt = 0;
            while (true) {
                try {
                    generator.generateAndPersist(reporter, facts, job.getInstruction());
                    break;
                } catch (AiGenerationAdmissionService.AdmissionDeniedException denied) {
                    if (denied.reason()
                            != AiGenerationAdmissionService.DenialReason.CONCURRENCY_LIMIT) {
                        throw denied;
                    }
                    sleepBeforeRetry();
                } catch (Exception failure) {
                    attempt++;
                    if (attempt >= MAX_TASK_ATTEMPTS || !retryableRatingFailure(failure)) {
                        throw failure;
                    }
                    sleepBeforeRetry();
                }
            }

            AiPlayerRatingTask latest = tasks.findById(task.getId()).orElse(task);
            if (latest.getStatus() == AiPlayerRatingTask.Status.CANCELLED) return;
            latest.setStatus(AiPlayerRatingTask.Status.SUCCEEDED);
            latest.setFinishedAt(Instant.now());
            tasks.save(latest);
        } catch (Exception error) {
            AiPlayerRatingTask latest = tasks.findById(task.getId()).orElse(task);
            if (latest.getStatus() == AiPlayerRatingTask.Status.CANCELLED) return;
            latest.setStatus(AiPlayerRatingTask.Status.FAILED);
            latest.setError(shortError(error));
            latest.setFinishedAt(Instant.now());
            tasks.save(latest);
        }
    }

    private boolean retryableRatingFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof net.warp_scores.warpscores.ai.provider.LlmProviderException provider
                    && provider.retryable()) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("max_output_tokens")
                        || normalized.contains("contained no output_text")
                        || normalized.contains("player rating response was not valid json")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(CONCURRENCY_RETRY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Player rating worker interrupted", interrupted);
        }
    }

    private List<AiReporterDefinition> selectReporters(Collection<String> reporterIds) {
        Set<String> requested = reporterIds == null
                ? Set.of()
                : new LinkedHashSet<>(reporterIds);

        List<AiReporterDefinition> enabled = effectiveProfiles.enabledForRatings().stream()
                .map(AiReporterEffectiveProfileService.EffectiveReporter::definition)
                .toList();

        if (!requested.isEmpty()) {
            Set<String> enabledIds = enabled.stream()
                    .map(AiReporterDefinition::getId)
                    .collect(Collectors.toSet());
            Set<String> unknown = requested.stream()
                    .filter(id -> !enabledIds.contains(id))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown or disabled rating reporters: " + unknown);
            }
        }

        var stream = enabled.stream()
                .filter(reporter -> requested.isEmpty()
                        || requested.contains(reporter.getId()));
        if (requested.isEmpty()) {
            stream = stream.limit(TEMPORARY_DEFAULT_REPORTER_LIMIT);
        }
        return stream.toList();
    }

    private JobSnapshot snapshot(AiPlayerRatingJob job) {
        List<AiPlayerRatingTask> rows =
                tasks.findByJobIdOrderByReporterIdAsc(job.getId());

        int queued = 0;
        int running = 0;
        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        int cancelled = 0;
        List<TaskSnapshot> taskSnapshots = new ArrayList<>();

        for (AiPlayerRatingTask task : rows) {
            switch (task.getStatus()) {
                case QUEUED -> queued++;
                case RUNNING -> running++;
                case SUCCEEDED -> succeeded++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
                case CANCELLED -> cancelled++;
            }
            taskSnapshots.add(new TaskSnapshot(
                    task.getReporterId(),
                    task.getStatus(),
                    task.getError(),
                    task.getStartedAt(),
                    task.getFinishedAt()));
        }

        boolean complete = !rows.isEmpty() && queued == 0 && running == 0;
        if (rows.isEmpty() && job.getReporterCount() == 0) complete = true;

        return new JobSnapshot(
                job.getId(),
                job.getMatchId(),
                job.getPlayerCount(),
                job.getReporterCount(),
                queued,
                running,
                succeeded,
                failed,
                skipped,
                cancelled,
                complete,
                job.getCreatedAt(),
                taskSnapshots);
    }

    private static String shortError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    public record TaskSnapshot(
            String reporterId,
            AiPlayerRatingTask.Status status,
            String error,
            Instant startedAt,
            Instant finishedAt) {}

    public record JobSnapshot(
            String jobId,
            String matchId,
            int playerCount,
            int reporterCount,
            int queued,
            int running,
            int succeeded,
            int failed,
            int skipped,
            int cancelled,
            boolean complete,
            Instant createdAt,
            List<TaskSnapshot> tasks) {}
}
