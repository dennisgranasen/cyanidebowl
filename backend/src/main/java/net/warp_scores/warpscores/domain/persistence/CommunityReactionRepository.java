package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.CommunityReaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityReactionRepository extends MongoRepository<CommunityReaction, String> {
    Optional<CommunityReaction> findByTargetTypeAndTargetIdAndUserSubject(
            CommunityReaction.TargetType targetType, String targetId, String userSubject);
    List<CommunityReaction> findByTargetTypeAndTargetId(
            CommunityReaction.TargetType targetType, String targetId);
}
