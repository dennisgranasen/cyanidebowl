package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "stageSource")
public class StageSource {
    @Id
    private String id;
    private String stageId;
    private String seasonId;
    private String leagueSystemId;
    private Identity sourceEntityId;
    private EntityType sourceType;
    private GameType game;
    private Platform platform;
    private String ruleset;
    private Integer firstIndex;
    private Integer lastIndex;
    private String firstId;
    private String lastId;
    private Boolean isArchived;
    private Long legacyCircuitId;
    private Long legacyCircuitLegId;
    private Integer legacyEntityIndex;
}
