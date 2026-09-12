package net.warp_scores.warpscores.ai.context.persistence;

import net.warp_scores.warpscores.ai.context.SubjectType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiSocialRelationshipRepository extends MongoRepository<AiSocialRelationship, String> {
    List<AiSocialRelationship> findByUserIdAndActiveTrueOrderByUpdatedAtDesc(
            Long userId, Pageable pageable);

    List<AiSocialRelationship> findBySubjectTypeAndSubjectIdAndActiveTrueOrderByUpdatedAtDesc(
            SubjectType subjectType, String subjectId, Pageable pageable);
}
