package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Rank;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends MongoRepository<Match, UUID> {
    List<Match> findByCompetitionId(UUID competitionId);

    Optional<Match> findTopByLeagueIdOrderByFinishedDesc(UUID leagueId);

    Optional<Match> findTopByTeamsContainsOrderByStartedDesc(Team team);

    @Aggregation(pipeline = {
            "{ '$match': { 'teams': { $elemMatch: { '_id': ?0 } } } }"
    })
    List<Match> findMatchesByTeamId(UUID teamId);
}
