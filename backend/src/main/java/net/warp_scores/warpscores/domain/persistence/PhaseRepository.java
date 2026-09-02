package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Phase;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PhaseRepository extends MongoRepository<Phase, String> {
    List<Phase> findBySeasonIdOrderBySequenceAsc(String seasonId);
}
