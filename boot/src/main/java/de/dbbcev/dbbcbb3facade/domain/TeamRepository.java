package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.domain.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends MongoRepository<Team, UUID> {
    List<Team> findByLeagueId(UUID leagueId);

    @Query("{competitionIds: ?0}")
    List<Team> findByCompetitionId(UUID competitionId);
}
