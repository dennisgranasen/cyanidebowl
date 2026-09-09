package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Document("aiReporterAssignments")
@CompoundIndex(name="match_unique", def="{'matchId':1}", unique=true)
public class AiReporterAssignment {
    @Id private String id;
    private String matchId;
    private List<String> reporterIds = new ArrayList<>();
    private double secondReportProbability;
    private Instant createdAt;
}
