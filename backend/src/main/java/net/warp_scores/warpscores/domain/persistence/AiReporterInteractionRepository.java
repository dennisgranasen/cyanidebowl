package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.AiReporterInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AiReporterInteractionRepository extends MongoRepository<AiReporterInteraction, String> {
    List<AiReporterInteraction> findByReporterIdOrderByCreatedAtDesc(String reporterId);
}
