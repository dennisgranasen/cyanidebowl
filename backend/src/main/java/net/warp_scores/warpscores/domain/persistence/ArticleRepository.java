package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Article;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {
    Optional<Article> findBySlug(String slug);
    Optional<Article> findByLegacySource(String legacySource);
    List<Article> findByStatusOrderByPublishedAtDesc(Article.Status status, Pageable pageable);
    List<Article> findByStatusAndLeagueSystemIdOrderByPublishedAtDesc(
            Article.Status status, String leagueSystemId, Pageable pageable);
    List<Article> findByStatusAndAuthorUserIdOrderByPublishedAtDesc(
            Article.Status status, Long authorUserId, Pageable pageable);
    List<Article> findByStatusAndTagsInOrderByPublishedAtDesc(
            Article.Status status, List<String> tags, Pageable pageable);
}
