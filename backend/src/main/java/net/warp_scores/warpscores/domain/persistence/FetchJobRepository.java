package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.scheduler.FetchJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FetchJobRepository extends MongoRepository<FetchJob, String> {
    List<FetchJob> findAllSortedByPriority();
}

