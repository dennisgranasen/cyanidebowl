package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.EditorialImageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EditorialImageRequestRepository extends MongoRepository<EditorialImageRequest, String> {
    List<EditorialImageRequest> findByStatusOrderByCreatedAtAsc(EditorialImageRequest.Status status);
}