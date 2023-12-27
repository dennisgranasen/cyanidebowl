package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.domain.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends MongoRepository<Match, UUID> {
}
