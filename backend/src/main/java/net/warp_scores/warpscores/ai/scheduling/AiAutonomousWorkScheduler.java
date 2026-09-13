package net.warp_scores.warpscores.ai.scheduling;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AiAutonomousWorkScheduler {
    private final AiAutonomousWorkQueue queue;
    private final Map<String, AiAutonomousWorkHandler> handlers;
    private final String leaseOwner = UUID.randomUUID().toString();

    @Value("${warpscores.ai.autonomous.batch-size:4}")
    private int batchSize = 4;

    @Value("${warpscores.ai.autonomous.lease-seconds:300}")
    private long leaseSeconds = 300;

    public AiAutonomousWorkScheduler(
            AiAutonomousWorkQueue queue,
            List<AiAutonomousWorkHandler> handlers) {
        this.queue = queue;
        this.handlers = new LinkedHashMap<>();
        for (AiAutonomousWorkHandler handler : handlers) {
            AiAutonomousWorkHandler previous =
                    this.handlers.put(handler.handlerKey(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate autonomous work handler key: " + handler.handlerKey());
            }
        }
    }

    @Scheduled(fixedDelayString = "${warpscores.ai.autonomous.poll-ms:15000}")
    public void tick() {
        int limit = Math.max(1, batchSize);
        Duration lease = Duration.ofSeconds(Math.max(30, leaseSeconds));

        for (int i = 0; i < limit; i++) {
            var claimed = queue.claimNext(leaseOwner, lease);
            if (claimed.isEmpty()) return;
            executeOne(claimed.get());
        }
    }

    void executeOne(AiAutonomousWorkItem item) {
        AiAutonomousWorkHandler handler = handlers.get(item.getHandlerKey());
        if (handler == null) {
            queue.markFailed(
                    item,
                    leaseOwner,
                    new IllegalStateException(
                            "No autonomous work handler registered for "
                                    + item.getHandlerKey()));
            return;
        }

        try {
            handler.execute(item);
            queue.markSucceeded(item, leaseOwner);
        } catch (Exception e) {
            log.warn(
                    "Autonomous AI work {} failed on attempt {}/{}: {}",
                    item.getCandidateKey(),
                    item.getAttempts(),
                    item.getMaxAttempts(),
                    e.getMessage());
            queue.markFailed(item, leaseOwner, e);
        }
    }

    String leaseOwnerForTest() {
        return leaseOwner;
    }
}
