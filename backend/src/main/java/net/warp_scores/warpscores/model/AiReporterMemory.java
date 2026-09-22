package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Getter @Setter
@Document("aiReporterMemories")
@CompoundIndex(name="reporter_active_created", def="{'reporterId':1,'active':1,'createdAt':-1}")
public class AiReporterMemory {
    public enum Type {
        ARTICLE_OPINION, PERSONAL_INTERACTION, GRIEVANCE, COMPLIMENT,
        PUBLIC_PREDICTION, EMBARRASSING_MISTAKE, RUNNING_JOKE,
        TEAM_OPINION, COACH_OPINION, PLAYER_OPINION
    }

    @Id private String id;
    private String reporterId;
    private Type type;
    private String targetReporterId;
    private String targetEntityId;
    private String summary;
    private double severity;
    private double decayRate;
    private boolean active = true;
    private Instant createdAt;
    private Instant lastReferencedAt;
    private Instant resolvedAt;
}
