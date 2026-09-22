package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DedicatedFanReconciliationJobRepository
        extends MongoRepository<DedicatedFanReconciliationJob, String> {
    Optional<DedicatedFanReconciliationJob>
            findFirstByStatusAndRequestedAtLessThanEqualOrderByRequestedAtAsc(
                    DedicatedFanReconciliationJob.Status status,
                    Instant requestedAt);
    List<DedicatedFanReconciliationJob>
            findByStatusAndStartedAtBefore(
                    DedicatedFanReconciliationJob.Status status,
                    Instant startedAt);
}
