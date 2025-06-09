package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
    public String get_id() {
        return leagueId != null && opus != null ? opus + "-" + leagueId : null;
    }

    private String leagueId;
    //private Integer oldLeagueId;
    
    private String leagueName;

    private Boolean collectionActive;

    private String platform;

    private Integer opus;
}
