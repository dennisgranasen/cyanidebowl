package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarpScoresUserRepository extends MongoRepository<WarpScoresUser, Long> {
    Optional<WarpScoresUser> findByEmail(String email);
}
