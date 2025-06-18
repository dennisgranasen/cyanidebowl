package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "identity")
@ToString(of = {"identity"})
public class CompetitionTeams {
    @Id
    public String get_id() {
        return identity != null ? identity.getId() : null;
    }
    public String getCompetitionId() { return identity != null ? identity.getId() : null; }   
    private final Identity identity;

    public CompetitionTeams(Identity identity) {
        this.identity = identity;
    }

    private List<String> teamIds;
}
