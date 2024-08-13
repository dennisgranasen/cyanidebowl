package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
public class WarpScoresUser {
    @Id
    private Long id;

    private UUID coachId;

    private String username;

    private String email;

    private String provider;

    private List<Long> adminForCircuits = new ArrayList<>();
    private List<UUID> adminForLeagues = new ArrayList<>();
    private List<UUID> adminForCompetitions = new ArrayList<>();
}
