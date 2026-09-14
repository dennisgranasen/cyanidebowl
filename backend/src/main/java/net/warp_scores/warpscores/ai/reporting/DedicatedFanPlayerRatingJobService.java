package net.warp_scores.warpscores.ai.reporting;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class DedicatedFanPlayerRatingJobService {
    private final DedicatedFanPlayerRatingService generator;
    private final AtomicInteger workerNo = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
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
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), matchId, fans.size());
        jobs.put(job.jobId, job);
        latestByMatch.put(matchId, job.jobId);
        for (AiCommunityMemberProfile fan : fans) {
            executor.submit(() -> runOne(job, fan, facts, instruction, force));
        }
        return snapshot(job);
    }

    public JobSnapshot latest(String matchId) {
        String id = latestByMatch.get(matchId);
        return id == null ? null : snapshot(jobs.get(id));
    }

    private void runOne(MutableJob job, AiCommunityMemberProfile fan, PlayerRatingFacts facts, String instruction, boolean force) {
        synchronized (job) { job.queued--; job.running++; }
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
                    job.succeeded, job.failed, job.queued == 0 && job.running == 0, Map.copyOf(job.errors));
        }
    }

    private static String shortError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) message = e.getClass().getSimpleName();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    @PreDestroy void shutdown() { executor.shutdownNow(); }

    private static final class MutableJob {
        private final String jobId;
        private final String matchId;
        private final int total;
        private int queued;
        private int running;
        private int succeeded;
        private int failed;
        private final Map<String, String> errors = new java.util.LinkedHashMap<>();
        private MutableJob(String jobId, String matchId, int total) {
            this.jobId = jobId; this.matchId = matchId; this.total = total; this.queued = total;
        }
    }

    public record Availability(int activeFanCount) {}
    public record JobSnapshot(String jobId, String matchId, int fanCount, int queued, int running,
                              int succeeded, int failed, boolean complete, Map<String, String> errors) {}
}
