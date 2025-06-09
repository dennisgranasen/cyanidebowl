package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = {"competitionId", "opus"})
public class CompetitionTeams {
    @Id
    public String get_id() {
        return competitionId != null && opus != null ? opus + "-" + competitionId : null;
    }

    private String competitionId;
    private Integer opus; // Opus is the version of the competition teams, used for compatibility with different game versions. 

    private List<String> teamIds;
}
