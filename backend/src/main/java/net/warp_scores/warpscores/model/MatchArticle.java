package net.warp_scores.warpscores.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "match_articles")
public class MatchArticle {
    public enum Status { DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED }
    public enum Kind { EDITORIAL, TEAM_REPORT, COACH_CONTRIBUTION }
    public enum AuthorType { HUMAN, AI }

    @Id
    private String id;
    private String matchId;
    private String leagueSystemId;
    private String seasonId;

    private String title;
    private String body;
    private Status status;
    private Kind kind;
    private AuthorType authorType;

    private String authorSubject;
    private Long authorUserId;
    private String authorDisplayName;

    /** Set only for an official report written by one of the two match coaches. */
    private String teamId;
    private String teamName;

    /** AI provenance. These fields are null for human-authored articles. */
    private String reporterId;
    private String reporterAlias;
    private String providerId;
    private String model;
    private String providerRequestId;
    private Integer inputTokens;
    private Integer outputTokens;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private Instant reviewedAt;
    private String reviewedBySubject;
}
