package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Document
public class League {
    @Id
    private UUID uuid;
    private String logo;
    private String name;
    private Integer teamCount;
    private String treasury;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private Map<CompetitionStatus, Long> countsByCompetitionStatus;
}
