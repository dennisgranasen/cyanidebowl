package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.model.MatchArticle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchArticleRichTextTest {
    @Test void richFormattingAndImagesAreStoredAlongsidePlainText() {
        MatchArticle article = new MatchArticle();
        String html = "<h2>Match &amp; result</h2><p><strong>A win</strong></p><p style=\"text-align:center\">More</p><img src=\"/image.png\">";
        MatchArticleService.applyBody(article, new MatchArticleService.ArticleInput("Title", "untrusted alternate text", html));
        assertEquals(html, article.getBodyHtml());
        assertEquals("Match & result\nA win\nMore", article.getBody());
    }
    @Test void legacyPlainTextIsNotInterpretedAsMarkup() {
        MatchArticle article = new MatchArticle(); article.setBodyHtml("<p>old</p>");
        MatchArticleService.applyBody(article, new MatchArticleService.ArticleInput("Title", "Score < 3 & a quote"));
        assertNull(article.getBodyHtml());
        assertEquals("Score < 3 & a quote", article.getBody());
    }
    @Test void unsafeElementsDoNotReachStoredArticle() {
        MatchArticle article = new MatchArticle();
        MatchArticleService.applyBody(article, new MatchArticleService.ArticleInput("Title", "", "<p onclick=\"bad()\">Text</p><script>bad()</script>"));
        assertEquals("<p>Text</p>", article.getBodyHtml());
        assertEquals("Text", article.getBody());
    }
}
