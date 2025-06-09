package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = {"id", "opus"})
@ToString(of = {"name", "id"})
public class League {
    @Id
    public String get_id() {
        return id != null && opus != null ? opus + "-" + id : null;
    }
    public String getLeagueId() { return id; }


    private String id;
    //private Integer oldId; // This is the old ID used in the legacy system, if applicable.
    private String logo;
    private String name;
    private Integer teamCount;
    private String treasury;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private Map<CompetitionStatus, Long> countsByCompetitionStatus;
    private Integer opus; // Opus is the version of the league, used for compatibility with different game versions.

}
