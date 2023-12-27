package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.domain.model.LeagueCollection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeagueCollectionRepository extends MongoRepository<LeagueCollection, UUID> {

    List<LeagueCollection> findByCollectionActive(Boolean collectionActive);
}
