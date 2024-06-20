package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends MongoRepository<Team, UUID> {
    @Query("{leagueIds: ?0}")
    List<Team> findByLeagueId(UUID leagueId);

    @Query("{competitionIds: ?0}")
    List<Team> findByCompetitionId(UUID competitionId);
}
