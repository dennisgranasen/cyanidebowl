package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Getter @Setter
@Document("aiReporterInteractions")
public class AiReporterInteraction {
    public enum Type { LIKE, DISLIKE, COMMENT, REPLY }
    public enum Status { PLANNED, GENERATING, PUBLISHED, SKIPPED, FAILED }

    @Id private String id;
    private String reporterId;
    private String articleId;
    private String parentCommentId;
    private String targetReporterId;
    private Type type;
    private Status status = Status.PLANNED;
    private String generatedText;
    private String publishedCommentId;
    private Instant createdAt;
    private Instant publishedAt;
}
