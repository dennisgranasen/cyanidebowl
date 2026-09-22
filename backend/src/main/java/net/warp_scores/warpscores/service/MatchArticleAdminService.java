package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.model.MatchArticle;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MatchArticleAdminService {
    private final MatchArticleRepository articles;
    private final UserPermissionService permissions;

    public List<MatchArticle> rejected(Authentication auth) {
        requireSiteAdmin(auth);
        return articles.findByStatusOrderByUpdatedAtDesc(MatchArticle.Status.REJECTED);
    }

    public void deleteRejected(Authentication auth, String articleId) {
        requireSiteAdmin(auth);
        MatchArticle article = articles.findById(articleId)
                .orElseThrow(() -> new NoSuchElementException("Match article not found"));
        if (article.getStatus() != MatchArticle.Status.REJECTED) {
            throw new IllegalStateException("Only rejected match articles may be permanently deleted");
        }
        articles.delete(article);
    }

    private void requireSiteAdmin(Authentication auth) {
        if (!permissions.isSiteAdmin(auth)) {
            throw new AccessDeniedException("Site administrator permission required");
        }
    }
}
