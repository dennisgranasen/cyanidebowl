package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.LeagueCollection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeagueCollectionRepository extends MongoRepository<LeagueCollection, Identity> {

    List<LeagueCollection> findByCollectionActive(Boolean collectionActive);

}
