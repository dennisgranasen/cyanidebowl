package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
public class CompetitionTeams {
    @Id
    private UUID competitionUuid;
    private Integer oldCompetitionId; // This is the old ID used in the legacy system, if applicable.
    private Integer opus; // Opus is the version of the competition teams, used for compatibility with different game versions. 

    private List<UUID> teamUuids;
}
