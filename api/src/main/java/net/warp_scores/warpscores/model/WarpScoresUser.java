package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document
public class WarpScoresUser {
    @Id
    private Long id;

    private String[] coachIds;

    private String username;

    private String email;

    private String provider;

    /** Stable external identity (normally the Auth0 subject), never an email address. */
    private String authSubject;

    /** Convenience login name only; Steam secrets are deliberately never persisted. */
    private String steamUsername;

    private String steamId;

    private List<Long> adminForCircuits = new ArrayList<>();
    private List<String> adminForLeagues = new ArrayList<>();
    private List<String> adminForCompetitions = new ArrayList<>();
}
