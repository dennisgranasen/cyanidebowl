package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TeamRepository extends MongoRepository<Team, Identity> {
    @Query("{ 'competitionIds': ?0 }")
    List<Team> findByCompetitionId(Identity competitionId);
    
    @Query("{ 'leagueIds': ?0 }")
    List<Team> findByLeagueId(Identity leagueId);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Team> findByNameRegex(String name);

    @Query("{ 'competitionIds': { $in: ?0 } }")
    List<Team> findByCompetitionIdsIn(Collection<Identity> competitionIds);

    List<Team> findByNameContainingIgnoreCase(String name);
}
