package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = {"leagueId", "opus"})
@ToString(of = {"leagueName",  "leagueId"})
public class LeagueCollection {
    @Id
    private final Identity identity;

    public LeagueCollection(Identity identity) {
        this.identity = identity;
    }

    //private Integer oldLeagueId;
    
    private String leagueName;

    private Boolean collectionActive;

    private String platform;

}
