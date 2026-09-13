package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiCommunityMediaGenerationRequestRepository
        extends MongoRepository<AiCommunityMediaGenerationRequest, String> {
    List<AiCommunityMediaGenerationRequest>
            findByFanProfileIdOrderByCreatedAtDesc(String fanProfileId);

    java.util.Optional<AiCommunityMediaGenerationRequest>
            findFirstByStatusOrderByCreatedAtAsc(
                    AiCommunityMediaGenerationRequest.Status status);
}
