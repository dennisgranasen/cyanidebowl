package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends MongoRepository<Match, UUID> {
    @Query("{teams._id: ?0}")
    List<Match> findByTeamId(UUID teamId);

    List<Match> findByCompetitionId(UUID competitionId);
}
