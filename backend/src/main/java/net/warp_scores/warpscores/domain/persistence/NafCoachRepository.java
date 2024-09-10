package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.NafCoach;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NafCoachRepository extends MongoRepository<NafCoach, Integer> {

    Optional<NafCoach> findByNafNameIgnoreCase(String nafCoachName);
}
