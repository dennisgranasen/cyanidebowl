package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.AiReporterAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface AiReporterAssignmentRepository extends MongoRepository<AiReporterAssignment, String> {
    Optional<AiReporterAssignment> findByMatchId(String matchId);
}
