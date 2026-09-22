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
@Document(collection = "registeredSource")
public class RegisteredSource {
    @Id private String id;
    private String seasonId;
    private String leagueSystemId;
    private Identity sourceEntityId;
    private EntityType sourceType;
    private GameType game;
    private Platform platform;
    private String ruleset;
    private Boolean collectionEnabled = true;
}
