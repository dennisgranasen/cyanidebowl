package net.warp_scores.warpscores.ai.context.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiMemoryRepository extends MongoRepository<AiMemoryEntry, String> {
    List<AiMemoryEntry> findByOwnerUserIdAndActiveTrueOrderByUpdatedAtDesc(
            Long ownerUserId, Pageable pageable);
}
