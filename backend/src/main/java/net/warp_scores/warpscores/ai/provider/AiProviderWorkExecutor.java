package net.warp_scores.warpscores.ai.provider;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AiProviderWorkExecutor {
    private final AiProviderProperties properties;
    private final Map<String, ProviderLane> lanes = new ConcurrentHashMap<>();
    private final Map<String, WorkItem> pending = new ConcurrentHashMap<>();
    private final AtomicInteger workerNumber = new AtomicInteger();
    private final AtomicLong order = new AtomicLong();

    public boolean hasCapacity(LlmProviderRouter.ModelTarget target) {
        return lane(target).hasCapacity();
    }

    public boolean submit(String workId, LlmProviderRouter.ModelTarget target, int priority, Runnable work) {
        if (workId == null || target == null || work == null) {
            throw new IllegalArgumentException("workId, provider target and work are required");
        }
        ProviderLane lane = lane(target);
        WorkItem item = new WorkItem(workId, priority, order.getAndIncrement(), lane, work);
        if (pending.putIfAbsent(workId, item) != null) return false;
        lane.executor.execute(item);
        return true;
    }

    private ProviderLane lane(LlmProviderRouter.ModelTarget target) {
        String group = target.quotaGroup();
        if (group == null || group.isBlank()) group = target.targetId();
        if (group == null || group.isBlank()) group = target.providerId();
        String targetId = target.targetId();
        return lanes.computeIfAbsent(group, key -> createLane(key, targetId));
    }

    public boolean reprioritize(String workId, int priority) {
        WorkItem item = pending.get(workId);
        if (item == null || !item.lane.executor.remove(item)) return false;
        item.priority = priority;
        item.lane.executor.execute(item);
        return true;
    }

    public boolean remove(String workId) {
        WorkItem item = pending.get(workId);
        if (item == null || !item.lane.executor.remove(item)) return false;
        pending.remove(workId, item);
        return true;
    }

    private ProviderLane createLane(String group, String targetId) {
        int concurrency = 1;
        AiProviderProperties.QueueConfig queue = properties.getQuotaGroups().get(group);
        if (queue == null) {
            AiProviderProperties.TargetConfig target = properties.getTargets().get(targetId);
            if (target != null) queue = target.getQueue();
        }
        if (queue != null) concurrency = Math.max(1, queue.getConcurrency());
        int workers = concurrency;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workers, workers, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(), task -> {
                    Thread thread = new Thread(task,
                            "ai-provider-work-" + group + "-" + workerNumber.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        return new ProviderLane(executor, workers);
    }

    @PreDestroy
    public void shutdown() {
        lanes.values().forEach(lane -> lane.executor.shutdownNow());
        pending.clear();
    }

    private record ProviderLane(ThreadPoolExecutor executor, int concurrency) {
        boolean hasCapacity() {
            return executor.getActiveCount() + executor.getQueue().size() < concurrency;
        }
    }

    private final class WorkItem implements Runnable, Comparable<WorkItem> {
        private final String id;
        private volatile int priority;
        private final long sequence;
        private final ProviderLane lane;
        private final Runnable work;

        private WorkItem(String id, int priority, long sequence, ProviderLane lane, Runnable work) {
            this.id = id;
            this.priority = priority;
            this.sequence = sequence;
            this.lane = lane;
            this.work = work;
        }

        @Override
        public void run() {
            try {
                work.run();
            } finally {
                pending.remove(id, this);
            }
        }

        @Override
        public int compareTo(WorkItem other) {
            int byPriority = Integer.compare(other.priority, priority);
            return byPriority != 0 ? byPriority : Long.compare(sequence, other.sequence);
        }
    }
}
