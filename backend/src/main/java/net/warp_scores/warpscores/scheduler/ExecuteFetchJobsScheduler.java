package net.warp_scores.warpscores.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.persistence.FetchJobRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteFetchJobsScheduler {

    private final FetchJobRepository fetchJobRepository;

    private final CyanideApiProperties cyanideApiProperties;

    public void executeFetchJobs() {
        List<FetchJob> fetchJobs = fetchJobRepository.findAllSortedByPriority();
        for (FetchJob fetchJob : fetchJobs) {
            log.info("Executing fetch job: {}", fetchJob);
            if (cyanideApiProperties.isJobExecutionSchedulerActive()) {
                // fetchJobExecutor.execute(fetchJob.getRequest());
                log.info("Executing Job {}...", fetchJob);
            } else {
                log.info("JobExecutionScheduler deactivated by configuration. Skipping execution.");
            }
            fetchJobRepository.delete(fetchJob);
        }
    }
}
