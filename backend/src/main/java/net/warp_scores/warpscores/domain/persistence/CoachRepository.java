package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Coach;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CoachRepository extends MongoRepository<Coach, UUID> {
    /**
     * Find a coach by their old ID and opus.
     *
     * @param oldId the old ID of the coach
     * @param opus  the opus number
     * @return the Coach if found, or null if not found
     */
    Coach findByOldIdAndOpus(Integer oldId, Integer opus);
}
