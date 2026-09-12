package net.warp_scores.warpscores.ai.context.persistence;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.ai.context.SubjectType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A current deterministic relationship, separate from canonical user identity. */
@Getter
@Setter
@Document(collection = "ai_social_relationship")
public class AiSocialRelationship {
    public enum Type {
        TEAM_AFFINITY,
        COACH_IDENTITY,
        LEAGUE_MEMBERSHIP,
        AFFILIATION
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
    private Instant createdAt;
    private Instant updatedAt;
}
