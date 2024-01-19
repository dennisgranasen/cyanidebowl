package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.domain.model.League;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeagueRepository extends MongoRepository<League, UUID> {
}
