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
            findFirstByStatusAndNextAttemptAtLessThanEqualOrderByPriorityDescCreatedAtAsc(
                    AiCommunityMediaGenerationRequest.Status status,
                    java.time.Instant nextAttemptAt);

    java.util.List<AiCommunityMediaGenerationRequest>
            findByStatusInOrderByPriorityDescCreatedAtAsc(
                    java.util.List<AiCommunityMediaGenerationRequest.Status> statuses);

    long countByStatus(AiCommunityMediaGenerationRequest.Status status);

    java.util.List<AiCommunityMediaGenerationRequest>
            findByStatusAndStartedAtBefore(
                    AiCommunityMediaGenerationRequest.Status status,
                    java.time.Instant startedAt);
}
