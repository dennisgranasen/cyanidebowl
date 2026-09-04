package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplaySweeperWorker {
    private final ReplaySweeperService service;

    @Async
    @EventListener
    public void run(ReplaySweepRequestedEvent ignored) {
        service.executeClaimed();
    }
}
