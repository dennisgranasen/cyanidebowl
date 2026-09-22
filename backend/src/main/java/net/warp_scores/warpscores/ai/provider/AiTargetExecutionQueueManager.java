package net.warp_scores.warpscores.ai.provider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Executes text-generation work through quota-scoped queues.
 *
 * The public class name is retained for source/API compatibility, but the queue
 * identity is the configured quota-group rather than the target. Targets are
 * job metadata used to select provider/model.
 */
@Service
@RequiredArgsConstructor
public class AiTargetExecutionQueueManager {
    private final AiProviderProperties properties;
    private final LlmProviderRegistry registry;
    private final AiGenerationTraceStore traceStore;
    private final AiGenerationAdmissionService admission;

    private final Map<String, QuotaQueue> queues = new ConcurrentHashMap<>();

    @PostConstruct
    void initialize() {
        properties.getTargets().forEach((targetId, cfg) -> {
            if (cfg.getModality() != AiProviderProperties.Modality.TEXT) return;
            String group = quotaGroup(targetId, cfg);
            queue(group, cfg).registerTarget(targetId, cfg);
        });
    }

    public CanonicalLlmResponse execute(
            LlmProviderRouter.ModelTarget target,
            String agentId,
            CanonicalLlmRequest request,
            int priority) {
        if (target.targetId() == null) return direct(target, agentId, request);

        AiProviderProperties.TargetConfig cfg = properties.getTargets().get(target.targetId());
        if (cfg == null || cfg.getModality() != AiProviderProperties.Modality.TEXT) {
            throw new IllegalStateException("Unknown/non-text AI target: " + target.targetId());
        }

        String group = target.quotaGroup() == null || target.quotaGroup().isBlank()
                ? quotaGroup(target.targetId(), cfg)
                : target.quotaGroup();

        QuotaQueue q = queue(group, cfg);
        q.registerTarget(target.targetId(), cfg);

        QueuedCall call = new QueuedCall(
                UUID.randomUUID().toString(),
                priority,
                target,
                agentId,
                request);
        q.enqueue(call);

        try {
            return call.result.get();
        } catch (InterruptedException e) {
            q.cancel(call);
            Thread.currentThread().interrupt();
            throw new CancellationException("AI execution caller cancelled");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new CompletionException(cause);
        }
    }

    public List<QueueSnapshot> snapshots() {
        return queues.values().stream()
                .sorted(Comparator.comparing(q -> q.groupId))
                .map(QuotaQueue::snapshot)
                .toList();
    }

    /** Advisory wait for a newly submitted job; never exposes prompts or agent identities. */
    public WaitEstimate estimate(String targetId, int priority) {
        QuotaQueue q = queueForTarget(targetId);
        if (q == null) return new WaitEstimate(0, 0, null, null);

        synchronized (q.monitor) {
            Instant now = Instant.now();
            int ahead = (int) q.pending.stream()
                    .filter(c -> !c.result.isDone()
                            && c.priority >= priority
                            && (c.availableAt == null || !c.availableAt.isAfter(now)))
                    .count();

            Instant blocked = q.blockedUntil();
            long samples = q.succeeded.get();
            Long seconds = samples < 3 ? null : Math.max(0, (long) Math.ceil(
                    (ahead + q.running.get())
                            * (q.completedMillis.get() / (double) samples)
                            / (Math.max(1, q.queueConfig.getConcurrency()) * 1000)));

            if (seconds != null && blocked != null) {
                seconds += Math.max(0, Duration.between(now, blocked).toSeconds());
            }
            return new WaitEstimate(ahead, q.running.get(), seconds, blocked);
        }
    }

    public record WaitEstimate(int ahead, int running, Long estimatedWaitSeconds, Instant notBefore) {}

    /**
     * queueOrTarget accepts the new quota-group id and, for compatibility,
     * a target id which is resolved to its quota-group.
     */
    public boolean reprioritize(String queueOrTarget, String job, int priority) {
        validatePriority(priority);
        QuotaQueue q = resolveQueue(queueOrTarget);
        return q != null && q.reprioritize(job, priority);
    }

    public boolean remove(String queueOrTarget, String job) {
        QuotaQueue q = resolveQueue(queueOrTarget);
        return q != null && q.remove(job);
    }

    public int clear(String queueOrTarget) {
        QuotaQueue q = resolveQueue(queueOrTarget);
        return q == null ? 0 : q.clear();
    }

    @PreDestroy
    void stop() {
        queues.values().forEach(QuotaQueue::stop);
    }

    private CanonicalLlmResponse direct(
            LlmProviderRouter.ModelTarget target,
            String agent,
            CanonicalLlmRequest request) {
        admission.acquire(agent, request);
        try {
            return registry.require(target.providerId()).generate(request);
        } finally {
            admission.release();
        }
    }

    private QuotaQueue queue(String group, AiProviderProperties.TargetConfig fallbackTarget) {
        return queues.computeIfAbsent(
                group,
                ignored -> new QuotaQueue(group, quotaQueueConfig(group, fallbackTarget)));
    }

    private QuotaQueue resolveQueue(String queueOrTarget) {
        QuotaQueue direct = queues.get(queueOrTarget);
        if (direct != null) return direct;
        return queueForTarget(queueOrTarget);
    }

    private QuotaQueue queueForTarget(String targetId) {
        AiProviderProperties.TargetConfig cfg = properties.getTargets().get(targetId);
        if (cfg == null || cfg.getModality() != AiProviderProperties.Modality.TEXT) return null;
        return queues.get(quotaGroup(targetId, cfg));
    }

    private AiProviderProperties.QueueConfig quotaQueueConfig(
            String group,
            AiProviderProperties.TargetConfig fallbackTarget) {
        AiProviderProperties.QueueConfig configured = properties.getQuotaGroups().get(group);
        return configured != null ? configured : fallbackTarget.getQueue();
    }

    private static String quotaGroup(String targetId, AiProviderProperties.TargetConfig cfg) {
        return cfg.getQuotaGroup() == null || cfg.getQuotaGroup().isBlank()
                ? targetId
                : cfg.getQuotaGroup();
    }

    private final class QuotaQueue {
        final String groupId;
        final AiProviderProperties.QueueConfig queueConfig;
        final Object monitor = new Object();
        final List<QueuedCall> pending = new ArrayList<>();
        final List<Thread> workers = new ArrayList<>();
        final AtomicInteger running = new AtomicInteger();
        final AtomicLong succeeded = new AtomicLong();
        final AtomicLong failed = new AtomicLong();
        final AtomicLong completed = new AtomicLong();
        final AtomicLong completedMillis = new AtomicLong();
        final Deque<Instant> completedHistory = new ConcurrentLinkedDeque<>();
        final Map<String, QueuedCall> activeJobs = new ConcurrentHashMap<>();
        final Set<String> targetIds = ConcurrentHashMap.newKeySet();
        final Set<String> providers = ConcurrentHashMap.newKeySet();
        final Set<String> models = ConcurrentHashMap.newKeySet();
        final Instant historyStartedAt = Instant.now();

        volatile boolean stopping;
        volatile String lastError;
        volatile Integer lastStatusCode;
        volatile Instant lastErrorAt;
        volatile Instant providerBlockedUntil;
        volatile String throttleReason;

        QuotaQueue(String groupId, AiProviderProperties.QueueConfig queueConfig) {
            this.groupId = groupId;
            this.queueConfig = queueConfig;
            for (int i = 0; i < Math.max(1, queueConfig.getConcurrency()); i++) {
                Thread thread = new Thread(
                        this::loop,
                        "ai-quota-" + groupId + "-" + (i + 1));
                thread.setDaemon(true);
                workers.add(thread);
                thread.start();
            }
        }

        void registerTarget(String targetId, AiProviderProperties.TargetConfig cfg) {
            if (targetId != null) targetIds.add(targetId);
            if (cfg.getProvider() != null) providers.add(cfg.getProvider());
            if (cfg.getModel() != null) models.add(cfg.getModel());
        }

        void enqueue(QueuedCall call) {
            synchronized (monitor) {
                if (!call.result.isDone()) pending.add(call);
                monitor.notifyAll();
            }
        }

        void cancel(QueuedCall call) {
            synchronized (monitor) {
                call.result.cancel(false);
                pending.remove(call);
                call.status = JobStatus.CANCELLED;
                monitor.notifyAll();
            }
        }

        void loop() {
            while (!stopping && !Thread.currentThread().isInterrupted()) {
                try {
                    QueuedCall call = take();
                    if (call != null) run(call);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        QueuedCall take() throws InterruptedException {
            synchronized (monitor) {
                while (!stopping) {
                    Instant now = Instant.now();
                    Instant blocked = blockedUntil();

                    QueuedCall best = pending.stream()
                            .filter(c -> !c.result.isDone())
                            .filter(c -> c.availableAt == null || !c.availableAt.isAfter(now))
                            .filter(c -> blocked == null || !blocked.isAfter(now))
                            .max(Comparator
                                    .comparingInt((QueuedCall c) -> c.priority)
                                    .thenComparing(c -> c.createdAt, Comparator.reverseOrder()))
                            .orElse(null);

                    if (best != null) {
                        pending.remove(best);
                        best.status = JobStatus.RUNNING;
                        activeJobs.put(best.id, best);
                        return best;
                    }

                    Instant wake = blocked;
                    for (QueuedCall call : pending) {
                        if (call.availableAt != null
                                && (wake == null || call.availableAt.isBefore(wake))) {
                            wake = call.availableAt;
                        }
                    }

                    if (wake == null) {
                        monitor.wait();
                    } else {
                        monitor.wait(Math.max(1L, Duration.between(now, wake).toMillis()));
                    }
                }
                return null;
            }
        }

        void run(QueuedCall call) {
            running.incrementAndGet();
            boolean admitted = false;
            long start = System.nanoTime();

            try {
                try {
                    admission.acquire(call.agentId, call.request);
                    admitted = true;
                } catch (AiGenerationAdmissionService.AdmissionDeniedException denied) {
                    if (denied.reason() == AiGenerationAdmissionService.DenialReason.CONCURRENCY_LIMIT) {
                        call.status = JobStatus.QUEUED;
                        call.availableAt = Instant.now().plusMillis(500);
                        enqueue(call);
                        return;
                    }
                    throw denied;
                }

                LlmProvider provider = registry.require(call.target.providerId());
                if (!provider.isConfigured()) {
                    throw new IllegalStateException(
                            "AI provider is not configured: " + call.target.providerId());
                }

                call.attempts++;
                CanonicalLlmResponse response = provider.generate(call.request);
                long duration = elapsed(start);

                traceStore.recordSuccess(
                        call.agentId,
                        call.target.providerId(),
                        call.request,
                        response,
                        duration);

                call.status = JobStatus.SUCCEEDED;
                recordCompletion(duration);
                succeeded.incrementAndGet();
                call.result.complete(response);
            } catch (LlmProviderException e) {
                long duration = elapsed(start);
                traceStore.recordFailure(
                        call.agentId,
                        call.target.providerId(),
                        call.request,
                        e,
                        duration);
                remember(e);

                if (e.kind() == LlmProviderException.Kind.RATE_LIMIT) {
                    Instant retry = e.retryAt();
                    if (retry == null || !retry.isAfter(Instant.now())) {
                        retry = Instant.now().plus(backoff(Math.max(1, call.attempts)));
                    }

                    blockUntil(retry, e.getMessage());

                    // Provider/quota exhaustion is not a failure of this job.
                    // Do not consume an attempt. The job rejoins the shared
                    // quota queue and competes by priority when the block ends.
                    call.attempts = Math.max(0, call.attempts - 1);
                    call.status = JobStatus.RETRY_WAIT;
                    call.availableAt = retry;
                    call.lastError = shortError(e);
                    enqueue(call);
                    return;
                }

                call.status = JobStatus.FAILED;
                call.lastError = shortError(e);
                recordCompletion(duration);
                failed.incrementAndGet();
                call.result.completeExceptionally(e);
            } catch (RuntimeException e) {
                long duration = elapsed(start);
                traceStore.recordUnexpectedFailure(
                        call.agentId,
                        call.target.providerId(),
                        call.request,
                        e,
                        duration);
                lastError = shortError(e);
                lastErrorAt = Instant.now();
                call.status = JobStatus.FAILED;
                call.lastError = shortError(e);
                recordCompletion(duration);
                failed.incrementAndGet();
                call.result.completeExceptionally(e);
            } finally {
                if (admitted) admission.release();
                activeJobs.remove(call.id);
                running.decrementAndGet();
            }
        }

        boolean reprioritize(String id, int priority) {
            synchronized (monitor) {
                QueuedCall call = pending.stream()
                        .filter(item -> item.id.equals(id))
                        .findFirst()
                        .orElse(null);
                if (call == null) return false;
                call.priority = priority;
                monitor.notifyAll();
                return true;
            }
        }

        boolean remove(String id) {
            synchronized (monitor) {
                QueuedCall call = pending.stream()
                        .filter(item -> item.id.equals(id))
                        .findFirst()
                        .orElse(null);
                if (call == null) return false;
                pending.remove(call);
                call.status = JobStatus.CANCELLED;
                call.result.completeExceptionally(
                        new CancellationException("Removed from AI quota queue by admin"));
                monitor.notifyAll();
                return true;
            }
        }

        int clear() {
            synchronized (monitor) {
                List<QueuedCall> copy = new ArrayList<>(pending);
                pending.clear();
                copy.forEach(call -> {
                    call.status = JobStatus.CANCELLED;
                    call.result.completeExceptionally(
                            new CancellationException("AI quota queue cleared by admin"));
                });
                monitor.notifyAll();
                return copy.size();
            }
        }

        QueueSnapshot snapshot() {
            List<JobSnapshot> jobs;
            synchronized (monitor) {
                jobs = java.util.stream.Stream
                        .concat(pending.stream(), activeJobs.values().stream())
                        .distinct()
                        .sorted(Comparator
                                .comparingInt((QueuedCall c) -> c.priority)
                                .reversed()
                                .thenComparing(c -> c.createdAt))
                        .map(QueuedCall::snapshot)
                        .toList();
            }

            Instant now = Instant.now();
            pruneHistory(now);

            long hour = completedHistory.stream()
                    .filter(t -> !t.isBefore(now.minus(Duration.ofHours(1))))
                    .count();
            long day = completedHistory.size();
            long completedCount = completed.get();

            Long average = completedCount == 0
                    ? null
                    : Long.valueOf(Math.max(0, completedMillis.get() / completedCount));

            Instant blocked = blockedUntil();

            Instant earliest = jobs.stream()
                    .filter(job -> job.status() != JobStatus.RUNNING)
                    .map(job -> job.nextAttemptAt() == null ? now : job.nextAttemptAt())
                    .min(Instant::compareTo)
                    .orElse(null);

            Instant resume = earliest;
            if (resume != null && blocked != null && blocked.isAfter(resume)) {
                resume = blocked;
            }

            double observedHours = Math.max(
                    1.0 / 60.0,
                    Math.min(
                            24.0,
                            Duration.between(historyStartedAt, now).toMillis() / 3600000.0));
            double perHour = hour >= 3 ? hour : (day / observedHours);
            long outstanding = jobs.size();

            Long eta = outstanding == 0
                    ? Long.valueOf(0L)
                    : perHour <= 0
                            ? null
                            : Long.valueOf((long) Math.ceil(outstanding / perHour * 3600.0));

            if (eta != null && resume != null && resume.isAfter(now)) {
                eta += Duration.between(now, resume).toSeconds();
            }

            String providerSummary = providers.size() == 1
                    ? providers.iterator().next()
                    : String.join(", ", new TreeSet<>(providers));
            String modelSummary = models.size() == 1
                    ? models.iterator().next()
                    : "mixed";

            return new QueueSnapshot(
                    groupId,
                    providerSummary,
                    modelSummary,
                    groupId,
                    List.copyOf(new TreeSet<>(targetIds)),
                    Math.max(1, queueConfig.getConcurrency()),
                    (int) jobs.stream().filter(j -> j.status() != JobStatus.RUNNING).count(),
                    running.get(),
                    jobs.stream().filter(j -> j.status() == JobStatus.RETRY_WAIT).count(),
                    succeeded.get(),
                    failed.get(),
                    blocked,
                    throttleReason,
                    lastError,
                    lastStatusCode,
                    lastErrorAt,
                    historyStartedAt,
                    hour,
                    day,
                    average,
                    resume,
                    eta,
                    jobs);
        }

        void recordCompletion(long durationMs) {
            completedMillis.addAndGet(Math.max(0, durationMs));
            completed.incrementAndGet();
            completedHistory.addLast(Instant.now());
            pruneHistory(Instant.now());
        }

        void pruneHistory(Instant now) {
            Instant cutoff = now.minus(Duration.ofHours(24));
            while (true) {
                Instant first = completedHistory.peekFirst();
                if (first == null || !first.isBefore(cutoff)) return;
                completedHistory.pollFirst();
            }
        }

        synchronized void blockUntil(Instant time, String reason) {
            if (time != null
                    && (providerBlockedUntil == null || time.isAfter(providerBlockedUntil))) {
                providerBlockedUntil = time;
                throttleReason = reason;
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        }

        Instant blockedUntil() {
            Instant blocked = providerBlockedUntil;
            if (blocked != null && !blocked.isAfter(Instant.now())) {
                providerBlockedUntil = null;
                throttleReason = null;
                return null;
            }
            return blocked;
        }

        Duration backoff(int attempt) {
            long multiplier = 1L << Math.min(20, Math.max(0, attempt - 1));
            long raw = Math.min(
                    queueConfig.getMaxBackoff().toMillis(),
                    queueConfig.getBaseBackoff().toMillis() * multiplier);
            double jitter = Math.max(0, Math.min(1, queueConfig.getJitter()));
            double factor = 1 + ThreadLocalRandom.current().nextDouble(-jitter, jitter);
            return Duration.ofMillis(Math.max(1, Math.round(raw * factor)));
        }

        void remember(LlmProviderException e) {
            lastError = shortError(e);
            lastStatusCode = e.statusCode();
            lastErrorAt = Instant.now();
        }

        void stop() {
            stopping = true;
            synchronized (monitor) {
                monitor.notifyAll();
            }
            workers.forEach(Thread::interrupt);
        }
    }

    private static final class QueuedCall {
        final String id;
        volatile int priority;
        final LlmProviderRouter.ModelTarget target;
        final String agentId;
        final CanonicalLlmRequest request;
        final Instant createdAt = Instant.now();
        final CompletableFuture<CanonicalLlmResponse> result = new CompletableFuture<>();
        volatile JobStatus status = JobStatus.QUEUED;
        volatile int attempts;
        volatile Instant availableAt;
        volatile String lastError;

        QueuedCall(
                String id,
                int priority,
                LlmProviderRouter.ModelTarget target,
                String agentId,
                CanonicalLlmRequest request) {
            validatePriority(priority);
            this.id = id;
            this.priority = priority;
            this.target = target;
            this.agentId = agentId;
            this.request = request;
        }

        JobSnapshot snapshot() {
            return new JobSnapshot(
                    id,
                    request.taskType() == null ? null : request.taskType().name(),
                    target.targetId(),
                    target.providerId(),
                    target.model(),
                    agentId,
                    priority,
                    status,
                    attempts,
                    createdAt,
                    availableAt,
                    lastError);
        }
    }

    public enum JobStatus {
        QUEUED,
        RUNNING,
        RETRY_WAIT,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    public record JobSnapshot(
            String id,
            String task,
            String target,
            String provider,
            String model,
            String agentId,
            int priority,
            JobStatus status,
            int attempts,
            Instant createdAt,
            Instant nextAttemptAt,
            String lastError) {}

    public record QueueSnapshot(
            String target,
            String provider,
            String model,
            String quotaGroup,
            List<String> targets,
            int concurrency,
            int queued,
            int running,
            long retryWaiting,
            long succeeded,
            long failed,
            Instant blockedUntil,
            String throttleReason,
            String lastError,
            Integer lastStatusCode,
            Instant lastErrorAt,
            Instant historyStartedAt,
            long completedLastHour,
            long completedLast24Hours,
            Long averageDurationMs,
            Instant resumeAt,
            Long estimatedClearSeconds,
            List<JobSnapshot> jobs) {}

    private static long elapsed(long startedAtNanos) {
        return Math.max(0, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private static String shortError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return message.length() <= 700 ? message : message.substring(0, 700);
    }

    private static void validatePriority(int priority) {
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority must be 0..100");
        }
    }
}
