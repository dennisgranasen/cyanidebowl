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

    private String username;

    private String email;

    private String provider;

    /** Stable external identity (normally the Auth0 subject), never an email address. */
    private String authSubject;

    /** Convenience login name only; Steam secrets are deliberately never persisted. */
    private String steamUsername;

    private String steamId;

    /** Full site administrator. This supersedes every narrower administrator permission. */
    private Boolean siteAdmin = false;

    /** Administrator for every LeagueSystem, but not site-global administration. */
    private Boolean leagueAdmin = false;

    /** May register/import new leagues where the relevant controller supports it. */
    private Boolean registerLeague = false;

    /** LeagueSystems this user may administer when leagueAdmin/siteAdmin is false. */
    private List<String> adminForLeagueSystems = new ArrayList<>();
}
