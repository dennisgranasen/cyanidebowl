package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiAutonomousWorkQueue {
    private final MongoTemplate mongoTemplate;

    public record EnqueueRequest(
            String candidateKey,
            String handlerKey,
            AiAutonomousWorkItem.WorkKind kind,
            AiAutonomousWorkItem.Priority priority,
            String leagueSystemId,
            String actorId,
            String targetType,
            String targetId,
            Map<String, String> payload,
            Integer maxAttempts) {
    }

    public record QueueSnapshot(
            long queued,
            long running,
            long retryWaiting,
            long succeeded,
            long failed,
            Instant oldestPendingCreatedAt,
            List<AiAutonomousWorkItem> recentFailures,
            List<AiAutonomousWorkItem> pendingJobs,
            long completedLastHour,
            long completedLast24Hours,
            Instant nextEligibleAt,
            Long estimatedClearSeconds) {
    }

    public boolean enqueue(EnqueueRequest request) {
        validate(request);

        Instant now = Instant.now();
        AiAutonomousWorkItem item = new AiAutonomousWorkItem();
        item.setCandidateKey(request.candidateKey().trim());
        item.setHandlerKey(request.handlerKey().trim());
        item.setKind(request.kind());

        AiAutonomousWorkItem.Priority priority = request.priority() == null
                ? AiAutonomousWorkItem.Priority.AUTONOMOUS
                : request.priority();
        item.setPriority(priority);
        item.setPriorityRank(priority.rank());

        item.setStatus(AiAutonomousWorkItem.Status.QUEUED);
        item.setLeagueSystemId(trimToNull(request.leagueSystemId()));
        item.setActorId(trimToNull(request.actorId()));
        item.setTargetType(trimToNull(request.targetType()));
        item.setTargetId(trimToNull(request.targetId()));
        item.setPayload(request.payload() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.payload()));
        item.setMaxAttempts(
                request.maxAttempts() == null
                        ? 5
                        : Math.max(1, request.maxAttempts()));
        item.setNextAttemptAt(now);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        try {
            mongoTemplate.insert(item);
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    public Optional<AiAutonomousWorkItem> claimNext(
            String leaseOwner,
            Duration leaseDuration) {
        if (!StringUtils.hasText(leaseOwner)) {
            throw new IllegalArgumentException("leaseOwner is required");
        }
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }

        Instant now = Instant.now();
        Criteria runnable = new Criteria().orOperator(
                Criteria.where("status")
                        .in(AiAutonomousWorkItem.Status.QUEUED,
                                AiAutonomousWorkItem.Status.RETRY_WAIT)
                        .and("nextAttemptAt").lte(now),
                Criteria.where("status")
                        .is(AiAutonomousWorkItem.Status.RUNNING)
                        .and("leaseUntil").lt(now));

        Query query = new Query(runnable)
                .with(Sort.by(
                        Sort.Order.desc("priorityRank"),
                        Sort.Order.asc("createdAt")));

        Update update = new Update()
                .set("status", AiAutonomousWorkItem.Status.RUNNING)
                .set("leaseOwner", leaseOwner)
                .set("leaseUntil", now.plus(leaseDuration))
                .set("updatedAt", now)
                .inc("attempts", 1);

        AiAutonomousWorkItem claimed = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                AiAutonomousWorkItem.class);

        if (claimed == null) return Optional.empty();

        if (claimed.getAttempts() > claimed.getMaxAttempts()) {
            markTerminalFailure(
                    claimed,
                    leaseOwner,
                    "MaxAttemptsExceeded",
                    "Work item exceeded maxAttempts while reclaiming an expired lease");
            return Optional.empty();
        }

        return Optional.of(claimed);
    }

    public void markSucceeded(
            AiAutonomousWorkItem item,
            String leaseOwner) {
        Instant now = Instant.now();
        Query query = ownedRunning(item, leaseOwner);
        Update update = new Update()
                .set("status", AiAutonomousWorkItem.Status.SUCCEEDED)
                .set("completedAt", now)
                .set("updatedAt", now)
                .unset("leaseOwner")
                .unset("leaseUntil")
                .unset("lastErrorClass")
                .unset("lastErrorMessage");
        mongoTemplate.updateFirst(query, update, AiAutonomousWorkItem.class);
    }

    public void markFailed(
            AiAutonomousWorkItem item,
            String leaseOwner,
            Throwable failure) {
        String errorClass = failure == null
                ? "UnknownFailure"
                : failure.getClass().getSimpleName();
        String message = failure == null ? null : safeMessage(failure);

        if (item.getAttempts() >= item.getMaxAttempts()) {
            markTerminalFailure(item, leaseOwner, errorClass, message);
            return;
        }

        Instant now = Instant.now();
        Duration delay = retryBackoff(item.getAttempts());
        Query query = ownedRunning(item, leaseOwner);
        Update update = new Update()
                .set("status", AiAutonomousWorkItem.Status.RETRY_WAIT)
                .set("nextAttemptAt", now.plus(delay))
                .set("updatedAt", now)
                .set("lastErrorClass", errorClass)
                .set("lastErrorMessage", message)
                .unset("leaseOwner")
                .unset("leaseUntil");
        mongoTemplate.updateFirst(query, update, AiAutonomousWorkItem.class);
    }

    public List<AiAutonomousWorkItem> pendingItems() {
        Query query = new Query(Criteria.where("status").in(AiAutonomousWorkItem.Status.QUEUED, AiAutonomousWorkItem.Status.RETRY_WAIT))
                .with(Sort.by(Sort.Order.desc("priorityRank"), Sort.Order.asc("createdAt")));
        return mongoTemplate.find(query, AiAutonomousWorkItem.class);
    }

    public boolean reprioritize(String candidateKey, int priorityRank) {
        if (priorityRank < 0 || priorityRank > 1000) throw new IllegalArgumentException("priority must be 0..1000");
        Query query = new Query(Criteria.where("_id").is(candidateKey).and("status").in(AiAutonomousWorkItem.Status.QUEUED, AiAutonomousWorkItem.Status.RETRY_WAIT));
        return mongoTemplate.updateFirst(query, new Update().set("priorityRank", priorityRank).set("updatedAt", Instant.now()), AiAutonomousWorkItem.class).getModifiedCount() > 0;
    }

    public boolean removePending(String candidateKey) {
        Query query = new Query(Criteria.where("_id").is(candidateKey).and("status").in(AiAutonomousWorkItem.Status.QUEUED, AiAutonomousWorkItem.Status.RETRY_WAIT));
        return mongoTemplate.remove(query, AiAutonomousWorkItem.class).getDeletedCount() > 0;
    }

    public long clearPending() {
        Query query = new Query(Criteria.where("status").in(AiAutonomousWorkItem.Status.QUEUED, AiAutonomousWorkItem.Status.RETRY_WAIT));
        return mongoTemplate.remove(query, AiAutonomousWorkItem.class).getDeletedCount();
    }

        public long deleteItemsByStatusCompletedBefore(
                AiAutonomousWorkItem.Status status,
                Instant cutoff) {
            Criteria criteria = Criteria.where("status").is(status)
                                .and("completedAt").lt(cutoff);
                return mongoTemplate.remove(new Query(criteria), AiAutonomousWorkItem.class)
                                .getDeletedCount();
        }

    public QueueSnapshot snapshot() {
        long queued = count(AiAutonomousWorkItem.Status.QUEUED);
        long running = count(AiAutonomousWorkItem.Status.RUNNING);
        long retry = count(AiAutonomousWorkItem.Status.RETRY_WAIT);
        long succeeded = count(AiAutonomousWorkItem.Status.SUCCEEDED);
        long failed = count(AiAutonomousWorkItem.Status.FAILED);

        Query oldestQuery = new Query(
                Criteria.where("status").in(
                        AiAutonomousWorkItem.Status.QUEUED,
                        AiAutonomousWorkItem.Status.RETRY_WAIT))
                .with(Sort.by(Sort.Order.asc("createdAt")))
                .limit(1);
        AiAutonomousWorkItem oldest =
                mongoTemplate.findOne(oldestQuery, AiAutonomousWorkItem.class);

        Query failuresQuery = new Query(
                Criteria.where("status").is(AiAutonomousWorkItem.Status.FAILED))
                .with(Sort.by(Sort.Order.desc("updatedAt")))
                .limit(20);
        List<AiAutonomousWorkItem> pending = pendingItems();
        Instant now = Instant.now();
        long completedHour = countCompletedSince(now.minus(Duration.ofHours(1)));
        long completedDay = countCompletedSince(now.minus(Duration.ofHours(24)));
        Instant nextEligible = pending.stream()
                .map(AiAutonomousWorkItem::getNextAttemptAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
        long outstanding = queued + retry + running;
        double perHour = completedHour > 0 ? completedHour : completedDay / 24.0;
        Long eta = outstanding == 0
                ? 0L
                : perHour <= 0 ? null : (long) Math.ceil(outstanding / perHour * 3600.0);

        return new QueueSnapshot(
                queued,
                running,
                retry,
                succeeded,
                failed,
                oldest == null ? null : oldest.getCreatedAt(),
                mongoTemplate.find(failuresQuery, AiAutonomousWorkItem.class),
                pending,
                completedHour,
                completedDay,
                nextEligible,
                eta);
    }

    static Duration retryBackoff(int attempt) {
        int exponent = Math.max(0, Math.min(6, attempt - 1));
        long minutes = Math.min(60L, 1L << exponent);
        return Duration.ofMinutes(minutes);
    }

    private void markTerminalFailure(
            AiAutonomousWorkItem item,
            String leaseOwner,
            String errorClass,
            String message) {
        Instant now = Instant.now();
        Query query = ownedRunning(item, leaseOwner);
        Update update = new Update()
                .set("status", AiAutonomousWorkItem.Status.FAILED)
                .set("completedAt", now)
                .set("updatedAt", now)
                .set("lastErrorClass", errorClass)
                .set("lastErrorMessage", message)
                .unset("leaseOwner")
                .unset("leaseUntil");
        mongoTemplate.updateFirst(query, update, AiAutonomousWorkItem.class);
    }

    private Query ownedRunning(
            AiAutonomousWorkItem item,
            String leaseOwner) {
        if (item == null || !StringUtils.hasText(item.getCandidateKey())) {
            throw new IllegalArgumentException("claimed item is required");
        }
        return new Query(Criteria.where("_id").is(item.getCandidateKey())
                .and("status").is(AiAutonomousWorkItem.Status.RUNNING)
                .and("leaseOwner").is(leaseOwner));
    }

    private long count(AiAutonomousWorkItem.Status status) {
        return mongoTemplate.count(
                new Query(Criteria.where("status").is(status)),
                AiAutonomousWorkItem.class);
    }

    private long countCompletedSince(Instant cutoff) {
        Criteria criteria = Criteria.where("completedAt").gte(cutoff)
                .and("status").in(AiAutonomousWorkItem.Status.SUCCEEDED, AiAutonomousWorkItem.Status.FAILED);
        return mongoTemplate.count(new Query(criteria), AiAutonomousWorkItem.class);
    }

    private static void validate(EnqueueRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(request.candidateKey())) {
            throw new IllegalArgumentException("candidateKey is required");
        }
        if (!StringUtils.hasText(request.handlerKey())) {
            throw new IllegalArgumentException("handlerKey is required");
        }
        if (request.kind() == null) {
            throw new IllegalArgumentException("kind is required");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        if (message == null) return null;
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
