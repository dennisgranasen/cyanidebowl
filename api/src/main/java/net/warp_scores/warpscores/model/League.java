package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
@ToString(of = {"name", "id"})
public class League {

    @Id
    private final Identity id;

    public League(Identity id) {
        this.id = id;
    }

    public String getLeagueId() { return id != null ? id.getValue() : null; }

    private String logo;
    private String name;
    private Integer teamCount;
    private String treasury;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private Map<CompetitionStatus, Long> countsByCompetitionStatus;
}
