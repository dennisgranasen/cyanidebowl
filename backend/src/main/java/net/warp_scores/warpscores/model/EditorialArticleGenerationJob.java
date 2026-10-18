package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document("editorialArticleGenerationJobs")
@CompoundIndex(name = "editorial_article_generation_queue", def = "{'status': 1, 'priority': -1, 'createdAt': 1}")
public class EditorialArticleGenerationJob {
    @Id
    private String id;
    private String requestJson;
    private String requesterSubject;
    private List<String> requesterAuthorities = List.of();
    private Status status = Status.QUEUED;
    private int priority = 100;
    private int attempts;
    private String articleId;
    private String error;
    private Instant createdAt;
    private Instant startedAt;
    private Instant leaseUntil;
    private Instant completedAt;

    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
