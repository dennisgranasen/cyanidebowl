package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Getter @Setter
@Document("aiReporterRelationships")
@CompoundIndex(name="reporter_pair_unique", def="{'reporterA':1,'reporterB':1}", unique=true)
public class AiReporterRelationship {
    @Id private String id;
    private String reporterA;
    private String reporterB;
    private double affinity;
    private double respect;
    private double familiarity;
    private double rivalry;
    private Instant updatedAt;
}
