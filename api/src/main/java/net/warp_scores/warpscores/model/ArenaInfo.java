package net.warp_scores.warpscores.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@With
public class ArenaInfo {
    private Race race;
    private Integer coaches;
    private Integer teams;
    private Integer matches;
    private Integer wins;
    private Integer losses;
    private Integer activeRuns;
    private Integer completedRuns;
    private Integer failedRuns;
}
