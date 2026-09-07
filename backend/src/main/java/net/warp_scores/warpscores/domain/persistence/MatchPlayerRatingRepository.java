package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.MatchPlayerRating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchPlayerRatingRepository extends MongoRepository<MatchPlayerRating, String> {
    Optional<MatchPlayerRating> findByMatchIdAndPlayerIdAndRaterSubject(
            String matchId, String playerId, String raterSubject);
    List<MatchPlayerRating> findByMatchId(String matchId);
    List<MatchPlayerRating> findByMatchIdAndPlayerId(String matchId, String playerId);
    List<MatchPlayerRating> findBySeasonIdAndPlayerId(String seasonId, String playerId);
    List<MatchPlayerRating> findByPlayerId(String playerId);
}
