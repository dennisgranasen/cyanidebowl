package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.EditorialImageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EditorialImageRequestRepository extends MongoRepository<EditorialImageRequest, String> {
    Optional<EditorialImageRequest> findFirstByStatusOrderByCreatedAtAsc(EditorialImageRequest.Status status);
    List<EditorialImageRequest> findByStatusOrderByCreatedAtAsc(EditorialImageRequest.Status status);
}