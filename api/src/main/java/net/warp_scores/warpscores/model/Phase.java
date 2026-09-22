package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "phase")
public class Phase {
    @Id private String id;
    private String seasonId;
    private String leagueSystemId;
    private String name;
    private PhaseType type = PhaseType.OTHER;
    private Integer sequence;
}
