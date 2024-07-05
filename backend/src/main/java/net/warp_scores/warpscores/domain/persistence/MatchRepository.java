package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends MongoRepository<Match, UUID> {
    List<Match> findByCompetitionId(UUID competitionId);

    Optional<Match> findTopByLeagueIdOrderByStartedDesc(UUID leagueId);
}
