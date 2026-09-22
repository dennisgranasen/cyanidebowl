package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.RegisteredSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RegisteredSourceRepository extends MongoRepository<RegisteredSource, String> {
    List<RegisteredSource> findBySeasonId(String seasonId);
    List<RegisteredSource> findByLeagueSystemId(String leagueSystemId);
}
