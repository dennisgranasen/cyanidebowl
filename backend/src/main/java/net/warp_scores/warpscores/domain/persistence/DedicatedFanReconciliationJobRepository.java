package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DedicatedFanReconciliationJobRepository
        extends MongoRepository<DedicatedFanReconciliationJob, String> {
    List<DedicatedFanReconciliationJob>
            findByStatusAndRequestedAtLessThanEqual(
                    DedicatedFanReconciliationJob.Status status,
                    Instant requestedAt);
    List<DedicatedFanReconciliationJob>
            findByStatusAndStartedAtBefore(
                    DedicatedFanReconciliationJob.Status status,
                    Instant startedAt);
}
