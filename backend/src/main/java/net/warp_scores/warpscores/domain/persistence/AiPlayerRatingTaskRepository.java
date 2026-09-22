package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.AiPlayerRatingTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AiPlayerRatingTaskRepository
        extends MongoRepository<AiPlayerRatingTask, String> {
    List<AiPlayerRatingTask> findByJobIdOrderByReporterIdAsc(String jobId);
    List<AiPlayerRatingTask> findByStatusIn(Collection<AiPlayerRatingTask.Status> statuses);
}
