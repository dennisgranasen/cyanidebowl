package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.ReplaySweeperLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface ReplaySweeperLogRepository extends MongoRepository<ReplaySweeperLog,String> {
    List<ReplaySweeperLog> findTop50ByOrderByCreatedAtDesc();
}
