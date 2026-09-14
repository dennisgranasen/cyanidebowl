package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.AiPlayerRatingJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPlayerRatingJobRepository
        extends MongoRepository<AiPlayerRatingJob, String> {
    Optional<AiPlayerRatingJob> findFirstByMatchIdOrderByCreatedAtDesc(String matchId);
}
