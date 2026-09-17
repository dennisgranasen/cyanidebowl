package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.model.Article;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticleImageSubjectsTest {
    @Test void onlyRetainedImagesTriggerTagsAndDuplicatesAreCollapsed() {
        var mongo = mock(MongoTemplate.class);
        var service = new ArticleImageSubjects(mongo);
        String id = "11111111-2222-3333-4444-555555555555";
        var metadata = new ArticleImageSubjects.Image(id, "/picture.png", List.of(new Article.Association(Article.LinkType.FAN, "fan")), "A goblin in a fountain");
        when(mongo.findById(id, ArticleImageSubjects.Image.class)).thenReturn(metadata);
        String html = "<img src=\"/picture.png\" data-editorial-image=\"" + id + "\">";
        assertEquals(java.util.Set.of("fan"), service.tagged(html + html, Article.LinkType.FAN));
        verify(mongo, times(1)).findById(id, ArticleImageSubjects.Image.class);
        assertTrue(service.descriptions(html).contains("goblin in a fountain"));
        assertTrue(service.tagged("<p>Image removed</p>", Article.LinkType.FAN).isEmpty());
        assertTrue(service.tagged("<p data-editorial-image=\"" + id + "\">Not an image</p>", Article.LinkType.FAN).isEmpty());
    }
}
