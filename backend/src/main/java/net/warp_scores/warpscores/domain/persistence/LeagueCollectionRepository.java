package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.LeagueCollection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeagueCollectionRepository extends MongoRepository<LeagueCollection, UUID> {

    List<LeagueCollection> findByCollectionActive(Boolean collectionActive);
    List<LeagueCollection> findByOldLeagueIdAndOpus(Integer oldLeagueId, Integer opus);
}
