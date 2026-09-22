package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.CommunityComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends MongoRepository<CommunityComment, String> {
    List<CommunityComment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
            CommunityComment.TargetType targetType, String targetId);
    List<CommunityComment> findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            CommunityComment.TargetType targetType, String targetId, Pageable pageable);
    List<CommunityComment> findByAuthorUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long authorUserId, Pageable pageable);
    List<CommunityComment> findByLeagueSystemIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String leagueSystemId, Pageable pageable);
}
