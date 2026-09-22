package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.CoachClaim;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachClaimRepository extends MongoRepository<CoachClaim, String> {
    List<CoachClaim> findByAuthSubjectOrderByGameAscCoachNameAsc(String authSubject);
    List<CoachClaim> findByGameOrderByCoachNameAsc(CoachClaim.Game game);
    Optional<CoachClaim> findByGameAndCoachId(CoachClaim.Game game, String coachId);
}

