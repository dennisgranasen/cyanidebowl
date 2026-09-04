package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Document(collection = "replayAnalysis")
public class ReplayAnalysis {
    @Id private String matchId;
    private String gameId;
    private Integer parserVersion;
    private String replayVersion;
    private Date processedAt;
    private Integer stepCount;
    private Integer eventCount;
    private Integer sourceBoardStateCount;
    private Integer checkpointCount;
    private List<Map<String, Object>> diceRolls;
    private List<Map<String, Object>> resourceEvents;
    private List<Map<String, Object>> specialEvents;
    private List<Map<String, Object>> participantTotals;
    private Map<String, Integer> eventTypeCounts;
    private Map<String, Integer> dieValueCounts;
}
