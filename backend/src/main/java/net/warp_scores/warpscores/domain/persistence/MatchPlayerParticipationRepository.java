package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.MatchPlayerParticipation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchPlayerParticipationRepository extends MongoRepository<MatchPlayerParticipation, String> {
    List<MatchPlayerParticipation> findByMatchIdIn(List<String> matchIds);
    List<MatchPlayerParticipation> findByMatchIdOrderByTeamIdAscPlayerNameAsc(String matchId);
    long countByMatchId(String matchId);
}
