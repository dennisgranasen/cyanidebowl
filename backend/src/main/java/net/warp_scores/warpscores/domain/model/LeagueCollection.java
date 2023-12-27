package net.warp_scores.warpscores.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    private String leagueName;

    private Boolean collectionActive;
}
