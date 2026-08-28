package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Stage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageRepository extends MongoRepository<Stage, String> {
    List<Stage> findBySeasonIdOrderBySequenceAsc(String seasonId);
}
