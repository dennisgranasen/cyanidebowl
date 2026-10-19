package net.warp_scores.warpscores.ai.reporting;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.ai.provider.AiProviderWorkExecutor;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerRatingJobRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerRatingTaskRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import net.warp_scores.warpscores.model.AiPlayerRatingJob;
import net.warp_scores.warpscores.model.AiPlayerRatingTask;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPlayerRatingJobService {
    private static final int DEFAULT_REPORTER_LIMIT = 10;
    private static final int MAX_TASK_ATTEMPTS = 2;
    private static final long CONCURRENCY_RETRY_MS = 750L;

    private final AiReporterEffectiveProfileService effectiveProfiles;
    private final ReporterPlayerRatingService.PlayerRatingGenerator generator;
    private final AiPlayerMatchRatingRepository ratings;
    private final AiPlayerRatingJobRepository jobs;
    private final AiPlayerRatingTaskRepository tasks;
    private final ReplayAnalysisRepository replayAnalyses;
    private final PlayerRatingFactsBuilder factsBuilder;
    private final CopilotRatingQuotaUsageService quotaUsage;
    private final DedicatedFanPlayerRatingJobService fanRatingJobs;

    private final LlmProviderRouter providerRouter;
    private final AiProviderWorkExecutor providerWorkExecutor;
    private final Object taskLock = new Object();

    public synchronized void queueAfterReplayAnalysis(String matchId) {
        ReplayAnalysis analysis;
        PlayerRatingFacts facts;
        try {
            analysis = replayAnalyses.findById(matchId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Analyzed replay was not persisted for match " + matchId));
            facts = factsBuilder.build(analysis);
        } catch (Exception error) {
            log.warn("Could not prepare player ratings after analyzing match {}: {}",
                matchId, error.getMessage(), error);
            return;
        }
        if (facts.getPlayers() == null || facts.getPlayers().isEmpty()) return;

        try {
            Set<String> ratedReporterIds = ratings.findByMatchId(matchId).stream()
                    .filter(rating -> rating.getSourceType() != AiPlayerMatchRating.SourceType.FAN)
                    .map(AiPlayerMatchRating::getReporterId)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toSet());
            if (ratedReporterIds.size() < DEFAULT_REPORTER_LIMIT) {
            Set<String> activeReporterIds = tasks.findByMatchIdAndStatusIn(
                    matchId,
                    List.of(AiPlayerRatingTask.Status.QUEUED, AiPlayerRatingTask.Status.RUNNING))
                .stream()
                .map(AiPlayerRatingTask::getReporterId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

            List<AiReporterDefinition> eligible = effectiveProfiles.enabledForRatings().stream()
                .map(AiReporterEffectiveProfileService.EffectiveReporter::definition)
                .toList();
            List<AiReporterDefinition> selected = additionalReporters(
                eligible, ratedReporterIds, activeReporterIds,
                DEFAULT_REPORTER_LIMIT, ThreadLocalRandom.current());
            if (!selected.isEmpty()) {
                start(
                    matchId,
                    selected.stream().map(AiReporterDefinition::getId).toList(),
                    null,
                    false,
                    facts.getPlayers().size(),
                    "replay-analysis",
                    null,
                    null);
                log.info("Queued {} editorial player-rating reporters for analyzed match {}",
                    selected.size(), matchId);
            }
            }
        } catch (Exception error) {
            log.warn("Could not queue editorial player ratings after analyzing match {}: {}",
                    matchId, error.getMessage(), error);
        }

        fanRatingJobs.queueAfterReplayAnalysis(matchId, facts);
    }

    public JobSnapshot start(
            String matchId,
            Collection<String> reporterIds,
            String instruction,
            boolean force,
            int playerCount,
            String requestedBy,
            Integer priorityOverride) {
            return start(matchId, reporterIds, instruction, force, playerCount, requestedBy,
                priorityOverride, null);
            }

            public JobSnapshot start(
                String matchId,
                Collection<String> reporterIds,
                String instruction,
                boolean force,
                int playerCount,
                String requestedBy,
                Integer priorityOverride,
                String targetOverride) {
        List<AiReporterDefinition> selected = selectReporters(reporterIds);

        AiPlayerRatingJob job = new AiPlayerRatingJob();
        job.setId(UUID.randomUUID().toString());
        job.setMatchId(matchId);
        job.setRequestedBy(requestedBy);
        job.setInstruction(instruction);
        job.setForce(force);
        job.setPriority(priorityOverride);
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
            task.setTargetOverride(targetOverride);
            task.setPriority(priorityOverride == null ? 50 : priorityOverride);
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

        int queuedCount = (int) pending.stream()
            .filter(task -> task.getStatus() == AiPlayerRatingTask.Status.QUEUED).count();
        quotaUsage.start(job.getId(), matchId, CopilotRatingQuotaUsage.RatingKind.EDITORIAL,
            requestedBy, targetOverride, queuedCount);

        pending.stream()
                .filter(task -> task.getStatus() == AiPlayerRatingTask.Status.QUEUED)
                .forEach(this::submit);

        if (queuedCount == 0) completeQuotaUsageIfTerminal(job.getId());

        return snapshot(job);
    }

    public List<ReporterCandidate> unratedReporters(String matchId) {
        return effectiveProfiles.enabledForRatings().stream()
            .map(profile -> profile.definition())
                .filter(reporter -> ratings.findByMatchIdAndReporterId(matchId, reporter.getId()).isEmpty())
                .map(reporter -> new ReporterCandidate(reporter.getId(), reporter.getAlias()))
                .sorted(java.util.Comparator.comparing(
                    candidate -> candidate.alias(),
                    java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public Optional<JobSnapshot> cancel(String matchId, String jobId) {
        AiPlayerRatingJob job = jobs.findById(jobId).orElse(null);
        if (job == null || !matchId.equals(job.getMatchId())) return Optional.empty();

        synchronized (taskLock) {
            Instant now = Instant.now();
            for (AiPlayerRatingTask task : tasks.findByJobIdOrderByReporterIdAsc(jobId)) {
                if (task.getStatus() == AiPlayerRatingTask.Status.QUEUED) {
                    task.setStatus(AiPlayerRatingTask.Status.CANCELLED);
                    task.setError("Cancelled by user");
                    task.setFinishedAt(now);
                    tasks.save(task);
                    providerWorkExecutor.remove(task.getId());
                }
            }
        }
        completeQuotaUsageIfTerminal(jobId);
        return Optional.of(snapshot(job));
    }

    public boolean reprioritizeTask(String taskId, int priority) {
        validatePriority(priority);
        synchronized (taskLock) {
            AiPlayerRatingTask task = tasks.findById(taskId).orElse(null);
            if (task == null || task.getStatus() != AiPlayerRatingTask.Status.QUEUED) return false;
            task.setPriority(priority);
            tasks.save(task);
            providerWorkExecutor.reprioritize(taskId, priority);
            return true;
        }
    }

    public boolean moveTaskTarget(String taskId, String targetOverride) {
        synchronized (taskLock) {
            AiPlayerRatingTask task = tasks.findById(taskId).orElse(null);
            if (task == null || task.getStatus() != AiPlayerRatingTask.Status.QUEUED) return false;
            task.setTargetOverride(targetOverride);
            tasks.save(task);
            providerWorkExecutor.remove(taskId);
            submit(task);
            return true;
        }
    }

    public boolean removeQueuedTask(String taskId) {
        String jobId;
        synchronized (taskLock) {
            AiPlayerRatingTask task = tasks.findById(taskId).orElse(null);
            if (task == null || task.getStatus() != AiPlayerRatingTask.Status.QUEUED) return false;
            task.setStatus(AiPlayerRatingTask.Status.CANCELLED);
            task.setError("Removed by administrator");
            task.setFinishedAt(Instant.now());
            tasks.save(task);
            providerWorkExecutor.remove(taskId);
            jobId = task.getJobId();
        }
        completeQuotaUsageIfTerminal(jobId);
        return true;
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

    private void submit(AiPlayerRatingTask task) {
        var target = providerRouter.planForTask(
                task.getReporterId(),
                ContextTaskType.PLAYER_RATING,
                new LlmProviderRouter.ExecutionOverrides(task.getTargetOverride(), task.getPriority()))
                .primary();
        providerWorkExecutor.submit(
                task.getId(), target, task.getPriority() == null ? 50 : task.getPriority(),
                () -> runTask(task.getId()));
    }

    private void runTask(String taskId) {
        AiPlayerRatingTask task;
        synchronized (taskLock) {
            task = tasks.findById(taskId).orElse(null);
            if (task == null || task.getStatus() != AiPlayerRatingTask.Status.QUEUED) return;
            task.setStatus(AiPlayerRatingTask.Status.RUNNING);
            task.setStartedAt(Instant.now());
            task.setError(null);
            tasks.save(task);
        }

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
                        generator.generateAndPersist(reporter, facts, job.getInstruction(),
                            task.getPriority(), task.getTargetOverride());
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
            completeQuotaUsageIfTerminal(latest.getJobId());
        } catch (Exception error) {
            AiPlayerRatingTask latest = tasks.findById(task.getId()).orElse(task);
            if (latest.getStatus() == AiPlayerRatingTask.Status.CANCELLED) return;
            latest.setStatus(AiPlayerRatingTask.Status.FAILED);
            latest.setError(shortError(error));
            latest.setFinishedAt(Instant.now());
            tasks.save(latest);
            completeQuotaUsageIfTerminal(latest.getJobId());
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
            return randomSelection(stream.toList(), DEFAULT_REPORTER_LIMIT,
                    ThreadLocalRandom.current());
        }
        return stream.toList();
    }

    static List<AiReporterDefinition> randomSelection(
            List<AiReporterDefinition> reporters, int limit, java.util.Random random) {
        List<AiReporterDefinition> shuffled = new ArrayList<>(reporters);
        java.util.Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(Math.max(0, limit), shuffled.size()));
    }

    static List<AiReporterDefinition> additionalReporters(
            List<AiReporterDefinition> enabled,
            Set<String> ratedReporterIds,
            Set<String> activeReporterIds,
            int target,
            java.util.Random random) {
        Set<String> countedReporterIds = new LinkedHashSet<>(ratedReporterIds);
        countedReporterIds.addAll(activeReporterIds);
        int deficit = Math.max(0, target - countedReporterIds.size());
        List<AiReporterDefinition> eligible = enabled.stream()
                .filter(reporter -> reporter.getId() != null
                        && !countedReporterIds.contains(reporter.getId()))
                .toList();
        return randomSelection(eligible, deficit, random);
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
                    task.getPriority() == null ? 50 : task.getPriority(),
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

    private void completeQuotaUsageIfTerminal(String jobId) {
        List<AiPlayerRatingTask> rows = tasks.findByJobIdOrderByReporterIdAsc(jobId);
        if (rows.stream().anyMatch(task -> task.getStatus() == AiPlayerRatingTask.Status.QUEUED
                || task.getStatus() == AiPlayerRatingTask.Status.RUNNING)) return;
        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        int cancelled = 0;
        for (AiPlayerRatingTask task : rows) {
            switch (task.getStatus()) {
                case SUCCEEDED -> succeeded++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
                case CANCELLED -> cancelled++;
                default -> { }
            }
        }
        quotaUsage.complete(jobId, succeeded, failed, skipped, cancelled);
    }

    private static String shortError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private static void validatePriority(int priority) {
        if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority must be 0..100");
    }

    public record TaskSnapshot(
            String reporterId,
            AiPlayerRatingTask.Status status,
            int priority,
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

    public record ReporterCandidate(String id, String alias) {}
}
