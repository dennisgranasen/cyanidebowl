package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.DataCollection;
import net.warp_scores.warpscores.model.EntityType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataCollectionRepository extends MongoRepository<DataCollection, Identity> {

    List<DataCollection> findByCollectionType(EntityType type);
}
