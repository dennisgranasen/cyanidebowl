package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiCommunityMemberProfileRepository
        extends MongoRepository<AiCommunityMemberProfile, String> {

    List<AiCommunityMemberProfile> findByTeamIdOrderByOrdinalAsc(String teamId);

    Optional<AiCommunityMemberProfile> findByUserId(Long userId);
}
