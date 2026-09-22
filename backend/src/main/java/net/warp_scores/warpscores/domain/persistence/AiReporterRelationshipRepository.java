package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.AiReporterRelationship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface AiReporterRelationshipRepository extends MongoRepository<AiReporterRelationship, String> {
    List<AiReporterRelationship> findByReporterAOrReporterB(String reporterA, String reporterB);
    Optional<AiReporterRelationship> findByReporterAAndReporterB(String reporterA, String reporterB);
}
