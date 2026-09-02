package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "leagueSystem")
public class LeagueSystem {
    @Id
    private String id;
    private String name;
    private String slug;
    private Long legacyCircuitId;
    private List<Long> legacySupersedesCircuitIds = new ArrayList<>();
    private String status;
    private Boolean primary = false;
    private List<String> discoveryAliases = new ArrayList<>();
    private Boolean discoveryNotificationEnabled = false;
    private String discoveryNotificationEmail;
    private List<String> notifiedDiscoveryCandidateIds = new ArrayList<>();
}
