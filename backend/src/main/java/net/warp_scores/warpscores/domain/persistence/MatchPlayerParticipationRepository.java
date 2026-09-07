package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.MatchPlayerParticipation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchPlayerParticipationRepository extends MongoRepository<MatchPlayerParticipation, String> {
    List<MatchPlayerParticipation> findByMatchIdOrderByTeamIdAscPlayerNameAsc(String matchId);
    Optional<MatchPlayerParticipation> findByMatchIdAndPlayerId(String matchId, String playerId);
    long countByMatchId(String matchId);
}
