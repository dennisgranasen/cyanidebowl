package net.warp_scores.warpscores.scheduler;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.ReplaySweeperService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="replay-sweeper.scheduler-enabled",havingValue="true",matchIfMissing=true)
public class ReplaySweeperScheduler {
    private final ReplaySweeperService service;
    @Scheduled(fixedDelayString="${replay-sweeper.poll-delay-ms:60000}",initialDelayString="${replay-sweeper.initial-delay-ms:60000}")
    public void sweepWhenDue(){if(service.due())service.run();}
}
