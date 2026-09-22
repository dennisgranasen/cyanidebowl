package net.warp_scores.warpscores.ai.context.persistence;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.ai.context.SubjectType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A current deterministic relationship, separate from canonical user identity. */
@Getter
@Setter
@Document(collection = "ai_social_relationship")
public class AiSocialRelationship {
    public enum Type {
        TEAM_AFFINITY,
        COACH_IDENTITY,
        LEAGUE_MEMBERSHIP,
        AFFILIATION,
        TEAM_ATTITUDE,
        COACH_ATTITUDE
    }

    @Getter
    @Setter
    public static class Evidence {
        private String sourceContentId;
        private Double sentiment;
        private Double confidence;
        private String rationale;
        private Instant observedAt;
    }

    @Id
    private String id;
    private Long userId;
    private String userDisplayName;
    private Type type;
    private SubjectType subjectType;
    private String subjectId;
    private String subjectDisplayName;
    private Boolean active = true;

    /** Current derived attitude, -1 (hostile) .. +1 (adoring). */
    private Double sentiment;

    /** Confidence in the derived attitude, 0 .. 1. */
    private Double confidence;

    /** Most relevant current explanation for the attitude. */
    private String rationale;

    private Integer evidenceCount = 0;
    private List<Evidence> evidence = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
