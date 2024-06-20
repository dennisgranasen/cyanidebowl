package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusRepository extends MongoRepository<Status, String> {
}
