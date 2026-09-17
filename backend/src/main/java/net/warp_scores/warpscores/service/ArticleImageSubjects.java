package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Article;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/** Image tags survive editor round trips, and only images still in the published body cause replies. */
@Service
@RequiredArgsConstructor
public class ArticleImageSubjects {
    private final MongoTemplate mongo;
    private static final Pattern IMAGE = Pattern.compile("<img\\b[^>]*\\bdata-editorial-image=[\"']([a-f0-9-]{36})[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    @Document("editorialImageSubjects")
    public record Image(@Id String id, String url, List<Article.Association> associations, String description) {}

    public String save(String url, List<Article.Association> associations, String description) {
        String id = UUID.randomUUID().toString();
        mongo.save(new Image(id, url, List.copyOf(associations), description));
        return id;
    }
    public List<Image> images(String html) {
        if (html == null) return List.of();
        var result = new LinkedHashMap<String, Image>();
        var matcher = IMAGE.matcher(html);
        while (matcher.find() && result.size() < 100) {
            String id = matcher.group(1);
            if (result.containsKey(id)) continue;
            var image = mongo.findById(id, Image.class);
            if (image != null) result.put(id, image);
        }
        return List.copyOf(result.values());
    }
    public Set<String> tagged(String html, Article.LinkType type) {
        var ids = new LinkedHashSet<String>();
        for (var image : images(html)) for (var link : image.associations()) if (link.type() == type) ids.add(link.id());
        return ids;
    }
    public String descriptions(String html) {
        return images(html).stream().map(i -> "Image scene: " + i.description())
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
