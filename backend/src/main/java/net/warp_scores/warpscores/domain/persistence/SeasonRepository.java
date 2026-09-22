package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Season;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonRepository extends MongoRepository<Season, String> {
    List<Season> findByLeagueSystemIdOrderBySequenceAsc(String leagueSystemId);
}
