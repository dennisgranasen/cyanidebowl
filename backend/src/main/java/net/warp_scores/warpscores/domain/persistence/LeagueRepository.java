package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.League;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;

@Repository
public interface LeagueRepository extends MongoRepository<League, Identity> {
    /**
     * Find a league by its old ID.
     *
     * @param oldId the old ID of the league
     * @return an Optional containing the League if found, or empty if not found
     */
    //Optional<League> findByOldIdAndOpus(Integer oldId, Integer opus);  
}
