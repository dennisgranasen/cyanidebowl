package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPlayerMatchRatingRepository extends MongoRepository<AiPlayerMatchRating, String> {
    List<AiPlayerMatchRating> findByMatchId(String matchId);
    List<AiPlayerMatchRating> findByMatchIdAndReporterId(String matchId, String reporterId);
    List<AiPlayerMatchRating> findByPlayerIdOrderByGeneratedAtDesc(String playerId);
}
