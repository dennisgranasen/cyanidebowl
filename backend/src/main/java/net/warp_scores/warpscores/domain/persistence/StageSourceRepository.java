package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.StageSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageSourceRepository extends MongoRepository<StageSource, String> {
    List<StageSource> findByStageId(String stageId);
    List<StageSource> findByLeagueSystemId(String leagueSystemId);
}
