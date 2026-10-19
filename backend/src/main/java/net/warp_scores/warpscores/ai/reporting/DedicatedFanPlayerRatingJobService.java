package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.AiProviderWorkExecutor;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), matchId, fans.size(), 50,
                facts, instruction, force,
                fans.stream().map(fan -> fan.getDisplayName() == null ? fan.getId() : fan.getDisplayName()).toList());
        jobs.put(job.jobId, job);
        latestByMatch.put(matchId, job.jobId);
        quotaUsage.start(job.jobId, matchId, CopilotRatingQuotaUsage.RatingKind.FANS,
            requestedBy, targetOverride, fans.size());
        for (AiCommunityMemberProfile fan : fans) {
            String workId = workId(job, fan);
            job.workIds.add(workId);
            var target = providerRouter.planForTask(
                fan.getId(), ContextTaskType.PLAYER_RATING,
                new LlmProviderRouter.ExecutionOverrides(targetOverride, job.priority))
                .primary();
            providerWorkExecutor.submit(workId, target, job.priority,
                () -> runOne(job, fan, facts, instruction, force, targetOverride));
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

    public boolean removeQueued(String jobId) {
        MutableJob job = jobs.get(jobId);
        if (job == null) return false;
        synchronized (job) {
            if (job.running != 0 || job.queued == 0) return false;
            job.cancelled = true;
            int cancelledCount = job.queued;
            job.queued = 0;
                job.workIds.forEach(providerWorkExecutor::remove);
            job.usageFinalizing = true;
            quotaUsage.complete(job.jobId, job.succeeded, job.failed, 0, cancelledCount);
            jobs.remove(jobId, job);
            latestByMatch.remove(job.matchId, jobId);
            return true;
        }
    }

        private void runOne(
            MutableJob job,
            AiCommunityMemberProfile fan,
            PlayerRatingFacts facts,
            String instruction,
            boolean force,
            String targetOverride) {
        synchronized (job) {
            if (job.cancelled) return;
            job.queued--;
            job.running++;
        }
        try {
            generator.generateAndPersist(fan, facts, instruction, force, targetOverride);
            synchronized (job) { job.running--; job.succeeded++; }
        } catch (Exception e) {
            synchronized (job) {
                job.running--;
                job.failed++;
                job.errors.put(fan.getDisplayName() == null ? fan.getId() : fan.getDisplayName(), shortError(e));
            }
        }
        finalizeUsageIfComplete(job);
    }

    private void finalizeUsageIfComplete(MutableJob job) {
        int succeeded;
        int failed;
        synchronized (job) {
            if (job.usageFinalizing || job.queued != 0 || job.running != 0) return;
            job.usageFinalizing = true;
            succeeded = job.succeeded;
            failed = job.failed;
        }
        quotaUsage.complete(job.jobId, succeeded, failed, 0, 0);
    }

    private JobSnapshot snapshot(MutableJob job) {
        if (job == null) return null;
        synchronized (job) {
            return new JobSnapshot(job.jobId, job.matchId, job.total, job.queued, job.running,
                    job.succeeded, job.failed, job.queued == 0 && job.running == 0,
                    job.priority, Map.copyOf(job.errors));
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

    private static final class MutableJob {
        private final String jobId;
        private final String matchId;
        private final int total;
        private volatile int priority;
        private int queued;
        private int running;
        private int succeeded;
        private int failed;
        private boolean cancelled;
        private boolean usageFinalizing;
        private final Map<String, String> errors = new java.util.LinkedHashMap<>();
        private final PlayerRatingFacts facts;
        private final String instruction;
        private final boolean force;
        private final List<String> fanNames;
        private final List<String> workIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        private MutableJob(String jobId, String matchId, int total, int priority, PlayerRatingFacts facts,
                           String instruction, boolean force, List<String> fanNames) {
            this.jobId = jobId; this.matchId = matchId; this.total = total; this.queued = total;
            this.priority = priority;
            this.facts = facts;
            this.instruction = instruction;
            this.force = force;
            this.fanNames = List.copyOf(fanNames);
        }
    }

    public record Availability(int activeFanCount, int unratedFanCount) {}
    public record JobInputSnapshot(String matchId, PlayerRatingFacts facts, String instruction,
                                   boolean force, List<String> fanNames) {}
    public record JobSnapshot(String jobId, String matchId, int fanCount, int queued, int running,
                              int succeeded, int failed, boolean complete, int priority, Map<String, String> errors) {}
}
