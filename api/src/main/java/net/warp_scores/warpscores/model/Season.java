package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "season")
public class Season {
    @Id
    private String id;
    private String leagueSystemId;
    private Integer number;
    private String name;
    private Integer sequence;
    private Long legacyCircuitId;
    private Long legacyCircuitLegId;
    private Boolean isCollected;
}
