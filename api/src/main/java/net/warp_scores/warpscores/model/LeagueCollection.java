package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Document
@ToString
public class LeagueCollection {
    @Id
    private UUID leagueId;
    
    private int oldId; // This is the old ID used in the legacy system, if applicable.

    private String leagueName;

    private Boolean collectionActive;

    private String platform;

    private Integer opus;
}
