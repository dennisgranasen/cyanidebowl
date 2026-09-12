package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.MatchArticle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MatchArticleRepository extends MongoRepository<MatchArticle, String> {
    List<MatchArticle> findByMatchIdOrderByCreatedAtAsc(String matchId);
    List<MatchArticle> findByMatchIdAndStatusOrderByPublishedAtAsc(
            String matchId, MatchArticle.Status status);
    List<MatchArticle> findByStatusOrderByUpdatedAtDesc(MatchArticle.Status status);
}
