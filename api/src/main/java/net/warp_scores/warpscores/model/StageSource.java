package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

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
    /** Reference to the watched source. Null only for legacy inline sources. */
    private String registeredSourceId;
    private Identity sourceEntityId;
    private EntityType sourceType;
    private GameType game;
    private Platform platform;
    private String ruleset;
    private Integer firstIndex;
    private Integer lastIndex;
    private String firstId;
    private String lastId;
    private List<String> includedMatchIds = new ArrayList<>();
    private List<String> excludedMatchIds = new ArrayList<>();
    private Boolean isArchived;
    private Long legacyCircuitId;
    private Long legacyCircuitLegId;
    private Integer legacyEntityIndex;
}
