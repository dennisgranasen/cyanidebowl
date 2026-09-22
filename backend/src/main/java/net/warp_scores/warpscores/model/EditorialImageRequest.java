package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document("editorialImageRequests")
public class EditorialImageRequest {
    public enum ApprovalPolicy { AUTO, EDITORIAL, TECHNICIAN }
    public enum Status { COMMISSIONED, DEVELOPING, COMPLETED, FAILED, REJECTED }

    @Id private String id;
    private Status status;
    private ApprovalPolicy approvalPolicy;
    private String requestedBy;
    private String approvedBy;
    private String photographerId;
    private String prompt;
    private List<Article.Association> associations = List.of();
    private List<String> referenceImages = List.of();
    private boolean referencesDisabled;
    private String assetUrl;
    private String imageId;
    private String error;
    private Instant createdAt;
    private Instant completedAt;
}