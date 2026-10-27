package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.AiProviderWorkExecutor;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DedicatedFanPlayerRatingJobService {
    private final DedicatedFanPlayerRatingService generator;
    private final CopilotRatingQuotaUsageService quotaUsage;
    private final LlmProviderRouter providerRouter;
    private final AiProviderWorkExecutor providerWorkExecutor;
    private final AiPlayerRatingTargetSettings targetSettings;
    private final Map<String, MutableJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> latestByMatch = new ConcurrentHashMap<>();

    public Availability availability(PlayerRatingFacts facts) {
        List<AiCommunityMemberProfile> activeFans = generator.activeFansFor(facts);
        return new Availability(
                activeFans.size(),
                generator.unratedActiveFansFor(facts, activeFans).size());
    }

    public synchronized void queueAfterReplayAnalysis(String matchId, PlayerRatingFacts facts) {
        try {
            JobSnapshot current = latest(matchId);
            if (current != null && !current.complete()) return;
            if (generator.unratedActiveFansFor(facts).isEmpty()) return;

            JobSnapshot queued = start(matchId, facts, null, false, null, "replay-analysis");
            log.info("Queued fan player ratings for {} fans on analyzed match {}",
                    queued.fanCount(), matchId);
        } catch (Exception error) {
            log.warn("Could not queue fan player ratings after analyzing match {}: {}",
                    matchId, error.getMessage(), error);
        }
    }

    public JobSnapshot start(String matchId, PlayerRatingFacts facts, String instruction, boolean force) {
        return start(matchId, facts, instruction, force, null, null);
    }

    public JobSnapshot start(
            String matchId,
            PlayerRatingFacts facts,
            String instruction,
            boolean force,
            String targetOverride) {
            return start(matchId, facts, instruction, force, targetOverride, null);
            }

            public JobSnapshot start(
                String matchId,
                PlayerRatingFacts facts,
                String instruction,
                boolean force,
                String targetOverride,
                String requestedBy) {
        List<AiCommunityMemberProfile> fans = force
                ? generator.activeFansFor(facts)
                : generator.unratedActiveFansFor(facts);
        if (fans.isEmpty()) throw new IllegalStateException("No active Dedicated Fans are linked to either match team");
        String effectiveTarget = targetOverride == null ? targetSettings.fanTarget() : targetOverride;
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), matchId, fans.size(), 50,
            facts, instruction, force, effectiveTarget, requestedBy,
                fans.stream().map(fan -> fan.getDisplayName() == null ? fan.getId() : fan.getDisplayName()).toList());
        jobs.put(job.jobId, job);
        latestByMatch.put(matchId, job.jobId);
        quotaUsage.start(job.jobId, matchId, CopilotRatingQuotaUsage.RatingKind.FANS,
            requestedBy, effectiveTarget, fans.size());
        String quotaBatchId = hasText(effectiveTarget) && quotaUsage.isCopilotTarget(effectiveTarget)
                ? job.jobId : null;
        if (quotaBatchId != null) job.quotaBatches.put(quotaBatchId, new QuotaBatch(fans.size()));
        for (AiCommunityMemberProfile fan : fans) {
            String workId = workId(job, fan);
            job.workIds.add(workId);
            var target = providerRouter.planForTask(
                fan.getId(), ContextTaskType.PLAYER_RATING,
                new LlmProviderRouter.ExecutionOverrides(effectiveTarget, job.priority))
                .primary();
            synchronized (job) {
                job.queuedWork.put(workId, new QueuedFanWork(fan, effectiveTarget, quotaBatchId));
            }
            providerWorkExecutor.submit(workId, target, job.priority,
                () -> runOne(job, workId, fan));
        }
        return snapshot(job);
    }

    public JobSnapshot latest(String matchId) {
        String id = latestByMatch.get(matchId);
        return id == null ? null : snapshot(jobs.get(id));
    }
    public List<JobSnapshot> pendingSnapshots() {
        return jobs.values().stream().map(this::snapshot).filter(j -> !j.complete()).toList();
    }

    public JobInputSnapshot inputSnapshot(String jobId) {
        MutableJob job = jobs.get(jobId);
        if (job == null) return null;
        synchronized (job) {
            return new JobInputSnapshot(job.matchId, job.facts, job.instruction, job.force, job.fanNames);
        }
    }

    public boolean reprioritize(String jobId, int priority) {
        if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority must be 0..100");
        MutableJob job = jobs.get(jobId);
        if (job == null) return false;
        synchronized (job) {
            if (job.queued == 0 || job.cancelled) return false;
            job.priority = priority;
            job.workIds.forEach(workId -> providerWorkExecutor.reprioritize(workId, priority));
            return true;
        }
    }

    public int moveQueuedTarget(String jobId, String targetOverride) {
        MutableJob job = jobs.get(jobId);
        if (job == null) return 0;
        String normalizedTarget = normalizeTarget(targetOverride);
        List<QuotaCompletion> quotaCompletions = new ArrayList<>();
        int movedCount = 0;

        synchronized (job) {
            if (job.cancelled || job.queued == 0
                    || Objects.equals(normalizeTarget(job.targetOverride), normalizedTarget)) {
                return 0;
            }

            List<Map.Entry<String, QueuedFanWork>> candidates =
                    new ArrayList<>(job.queuedWork.entrySet());
            Map<String, LlmProviderRouter.ModelTarget> destinations = new java.util.HashMap<>();
            for (Map.Entry<String, QueuedFanWork> entry : candidates) {
                AiCommunityMemberProfile fan = entry.getValue().fan();
                destinations.put(entry.getKey(), providerRouter.planForTask(
                        fan.getId(), ContextTaskType.PLAYER_RATING,
                        new LlmProviderRouter.ExecutionOverrides(normalizedTarget, job.priority))
                        .primary());
            }

            String newQuotaBatchId = null;
            if (hasText(normalizedTarget) && quotaUsage.isCopilotTarget(normalizedTarget)) {
                newQuotaBatchId = job.jobId + ":target:" + UUID.randomUUID();
                quotaUsage.start(newQuotaBatchId, job.matchId, CopilotRatingQuotaUsage.RatingKind.FANS,
                        job.requestedBy, normalizedTarget, candidates.size());
                job.quotaBatches.put(newQuotaBatchId, new QuotaBatch(candidates.size()));
            }

            List<Map.Entry<String, QueuedFanWork>> moved = new ArrayList<>();
            for (Map.Entry<String, QueuedFanWork> entry : candidates) {
                String workId = entry.getKey();
                QueuedFanWork queuedWork = entry.getValue();
                LlmProviderRouter.ModelTarget destination = destinations.get(workId);
                boolean wasMoved = providerWorkExecutor.move(
                        workId,
                        destination,
                        job.priority,
                        () -> runOne(job, workId, queuedWork.fan()));
                if (wasMoved) moved.add(entry);
            }

            var movedWorkIds = moved.stream().map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());

            for (Map.Entry<String, QueuedFanWork> entry : candidates) {
                QueuedFanWork priorWork = entry.getValue();
                if (movedWorkIds.contains(entry.getKey())) {
                    job.queuedWork.put(entry.getKey(),
                            new QueuedFanWork(priorWork.fan(), normalizedTarget, newQuotaBatchId));
                    QuotaCompletion priorCompletion =
                            completeQuotaCall(job, priorWork.quotaBatchId(), false, true);
                    if (priorCompletion != null) quotaCompletions.add(priorCompletion);
                } else if (newQuotaBatchId != null) {
                    QuotaCompletion cancelledQuotaCall =
                            completeQuotaCall(job, newQuotaBatchId, false, true);
                    if (cancelledQuotaCall != null) quotaCompletions.add(cancelledQuotaCall);
                }
            }
            movedCount = moved.size();
            if (movedCount > 0) job.targetOverride = normalizedTarget;
        }

        completeQuotaCalls(quotaCompletions);
        return movedCount;
    }

    public boolean removeQueued(String jobId) {
        MutableJob job = jobs.get(jobId);
        if (job == null) return false;
        List<QuotaCompletion> quotaCompletions = new ArrayList<>();
        synchronized (job) {
            if (job.running != 0 || job.queued == 0) return false;
            job.cancelled = true;
            for (Map.Entry<String, QueuedFanWork> entry : job.queuedWork.entrySet()) {
                providerWorkExecutor.remove(entry.getKey());
                QuotaCompletion completion = completeQuotaCall(
                        job, entry.getValue().quotaBatchId(), false, true);
                if (completion != null) quotaCompletions.add(completion);
            }
            job.queuedWork.clear();
            job.queued = 0;
            job.workIds.forEach(providerWorkExecutor::remove);
            jobs.remove(jobId, job);
            latestByMatch.remove(job.matchId, jobId);
        }
        completeQuotaCalls(quotaCompletions);
        return true;
    }

    private void runOne(MutableJob job, String workId, AiCommunityMemberProfile fan) {
        QueuedFanWork queuedWork;
        synchronized (job) {
            if (job.cancelled) return;
            queuedWork = job.queuedWork.remove(workId);
            if (queuedWork == null) return;
            job.queued--;
            job.running++;
        }
        boolean succeeded = false;
        try {
            generator.generateAndPersist(fan, job.facts, job.instruction, job.force,
                    queuedWork.targetOverride());
            synchronized (job) { job.running--; job.succeeded++; }
            succeeded = true;
        } catch (Exception e) {
            synchronized (job) {
                job.running--;
                job.failed++;
                job.errors.put(fan.getDisplayName() == null ? fan.getId() : fan.getDisplayName(), shortError(e));
            }
        }
        QuotaCompletion quotaCompletion;
        synchronized (job) {
            quotaCompletion = completeQuotaCall(job, queuedWork.quotaBatchId(), succeeded, false);
        }
        if (quotaCompletion != null) completeQuotaCalls(List.of(quotaCompletion));
    }

    private QuotaCompletion completeQuotaCall(
            MutableJob job,
            String quotaBatchId,
            boolean succeeded,
            boolean cancelled) {
        if (quotaBatchId == null) return null;
        QuotaBatch batch = job.quotaBatches.get(quotaBatchId);
        if (batch == null || batch.pending == 0) return null;
        batch.pending--;
        if (cancelled) batch.cancelled++;
        else if (succeeded) batch.succeeded++;
        else batch.failed++;
        return batch.pending == 0
                ? new QuotaCompletion(quotaBatchId, batch.succeeded, batch.failed, batch.cancelled)
                : null;
    }

    private void completeQuotaCalls(List<QuotaCompletion> completions) {
        completions.forEach(completion -> quotaUsage.complete(
                completion.batchId(), completion.succeeded(), completion.failed(), 0, completion.cancelled()));
    }

    private JobSnapshot snapshot(MutableJob job) {
        if (job == null) return null;
        synchronized (job) {
            return new JobSnapshot(job.jobId, job.matchId, job.total, job.queued, job.running,
                    job.succeeded, job.failed, job.queued == 0 && job.running == 0,
                    job.priority, Map.copyOf(job.errors), job.targetOverride);
        }
    }

    private static String shortError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) message = e.getClass().getSimpleName();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private static String workId(MutableJob job, AiCommunityMemberProfile fan) {
        return job.jobId + ":fan:" + fan.getId();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeTarget(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private record QueuedFanWork(
            AiCommunityMemberProfile fan,
            String targetOverride,
            String quotaBatchId) {}

    private record QuotaCompletion(String batchId, int succeeded, int failed, int cancelled) {}

    private static final class QuotaBatch {
        private int pending;
        private int succeeded;
        private int failed;
        private int cancelled;

        private QuotaBatch(int pending) {
            this.pending = pending;
        }
    }

    private static final class MutableJob {
        private final String jobId;
        private final String matchId;
        private final int total;
        private final String requestedBy;
        private volatile int priority;
        private int queued;
        private int running;
        private int succeeded;
        private int failed;
        private boolean cancelled;
        private String targetOverride;
        private final Map<String, String> errors = new java.util.LinkedHashMap<>();
        private final PlayerRatingFacts facts;
        private final String instruction;
        private final boolean force;
        private final List<String> fanNames;
        private final List<String> workIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final Map<String, QueuedFanWork> queuedWork = new java.util.LinkedHashMap<>();
        private final Map<String, QuotaBatch> quotaBatches = new java.util.LinkedHashMap<>();
        private MutableJob(String jobId, String matchId, int total, int priority, PlayerRatingFacts facts,
                           String instruction, boolean force, String targetOverride, String requestedBy,
                           List<String> fanNames) {
            this.jobId = jobId; this.matchId = matchId; this.total = total; this.queued = total;
            this.priority = priority;
            this.facts = facts;
            this.instruction = instruction;
            this.force = force;
            this.targetOverride = normalizeTarget(targetOverride);
            this.requestedBy = requestedBy;
            this.fanNames = List.copyOf(fanNames);
        }
    }

    public record Availability(int activeFanCount, int unratedFanCount) {}
    public record JobInputSnapshot(String matchId, PlayerRatingFacts facts, String instruction,
                                   boolean force, List<String> fanNames) {}
    public record JobSnapshot(String jobId, String matchId, int fanCount, int queued, int running,
                              int succeeded, int failed, boolean complete, int priority,
                              Map<String, String> errors, String targetOverride) {}
}
