package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends MongoRepository<Team, Identity> {
    /*
    @Query("{ 'oldId' : ?0, 'opus' : ?1 }")
    Team findByOldIdAndOpus(Integer oldId, Integer opus);
    */
    @Query("{ 'competitionIds': ?0 }")
    List<Team> findByCompetitionId(Identity competitionId);
    
    @Query("{ 'leagueIds': ?0 }")
    List<Team> findByLeagueId(Identity leagueId);

    /*
    @Query("{ 'oldCompetitionIds': ?0, 'opus' : ?1 } }")
    List<Team> findByOldCompetitionIdAndOpus(Integer competitionId, Integer opus);
    
    @Query("{ 'oldLeagueIds': ?0, 'opus' : ?1 } }")
    List<Team> findByOldLeagueIdAndOpus(Integer leagueId, Integer opus);
    */

    List<Team> findByNameContainingIgnoreCase(String name);
}
