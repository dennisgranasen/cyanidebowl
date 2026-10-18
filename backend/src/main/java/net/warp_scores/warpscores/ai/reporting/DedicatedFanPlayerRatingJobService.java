package net.warp_scores.warpscores.ai.reporting;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class DedicatedFanPlayerRatingJobService {
    private final DedicatedFanPlayerRatingService generator;
    private final AtomicInteger workerNo = new AtomicInteger();
    private final AtomicLong taskOrder = new AtomicLong();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L,
            TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), runnable -> {
        Thread thread = new Thread(runnable, "fan-player-rating-" + workerNo.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, MutableJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> latestByMatch = new ConcurrentHashMap<>();

    public Availability availability(PlayerRatingFacts facts) {
        return new Availability(generator.activeFansFor(facts).size());
    }

    public JobSnapshot start(String matchId, PlayerRatingFacts facts, String instruction, boolean force) {
        List<AiCommunityMemberProfile> fans = generator.activeFansFor(facts);
        if (fans.isEmpty()) throw new IllegalStateException("No active Dedicated Fans are linked to either match team");
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), matchId, fans.size(), 50,
                facts, instruction, force,
                fans.stream().map(fan -> fan.getDisplayName() == null ? fan.getId() : fan.getDisplayName()).toList());
        jobs.put(job.jobId, job);
        latestByMatch.put(matchId, job.jobId);
        for (AiCommunityMemberProfile fan : fans) {
            executor.execute(new FanRatingTask(job, fan, facts, instruction, force, taskOrder.getAndIncrement()));
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
            List<Runnable> queued = executor.getQueue().stream()
                    .filter(task -> task instanceof FanRatingTask fanTask && fanTask.job == job)
                    .toList();
            List<Runnable> reordered = queued.stream().filter(executor.getQueue()::remove).toList();
            reordered.forEach(executor::execute);
            return true;
        }
    }

    public boolean removeQueued(String jobId) {
        MutableJob job = jobs.get(jobId);
        if (job == null) return false;
        synchronized (job) {
            if (job.running != 0 || job.queued == 0) return false;
            List<Runnable> queued = executor.getQueue().stream()
                    .filter(task -> task instanceof FanRatingTask fanTask && fanTask.job == job)
                    .toList();
            job.cancelled = true;
            job.queued = 0;
            queued.forEach(executor::remove);
            jobs.remove(jobId, job);
            latestByMatch.remove(job.matchId, jobId);
            return true;
        }
    }

    private void runOne(MutableJob job, AiCommunityMemberProfile fan, PlayerRatingFacts facts, String instruction, boolean force) {
        synchronized (job) {
            if (job.cancelled) return;
            job.queued--;
            job.running++;
        }
        try {
            generator.generateAndPersist(fan, facts, instruction, force);
            synchronized (job) { job.running--; job.succeeded++; }
        } catch (Exception e) {
            synchronized (job) {
                job.running--;
                job.failed++;
                job.errors.put(fan.getDisplayName() == null ? fan.getId() : fan.getDisplayName(), shortError(e));
            }
        }
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

    @PreDestroy void shutdown() { executor.shutdownNow(); }

    private final class FanRatingTask implements Runnable, Comparable<FanRatingTask> {
        private final MutableJob job;
        private final AiCommunityMemberProfile fan;
        private final PlayerRatingFacts facts;
        private final String instruction;
        private final boolean force;
        private final long order;

        private FanRatingTask(MutableJob job, AiCommunityMemberProfile fan, PlayerRatingFacts facts,
                              String instruction, boolean force, long order) {
            this.job = job;
            this.fan = fan;
            this.facts = facts;
            this.instruction = instruction;
            this.force = force;
            this.order = order;
        }

        @Override public void run() { runOne(job, fan, facts, instruction, force); }

        @Override public int compareTo(FanRatingTask other) {
            int byPriority = Integer.compare(other.job.priority, job.priority);
            return byPriority != 0 ? byPriority : Long.compare(order, other.order);
        }
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
        private final Map<String, String> errors = new java.util.LinkedHashMap<>();
        private final PlayerRatingFacts facts;
        private final String instruction;
        private final boolean force;
        private final List<String> fanNames;
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

    public record Availability(int activeFanCount) {}
    public record JobInputSnapshot(String matchId, PlayerRatingFacts facts, String instruction,
                                   boolean force, List<String> fanNames) {}
    public record JobSnapshot(String jobId, String matchId, int fanCount, int queued, int running,
                              int succeeded, int failed, boolean complete, int priority, Map<String, String> errors) {}
}
