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
    /** Missing in legacy documents means HUMAN. */
    private AccountType accountType = AccountType.HUMAN;
    /** Stable external identity (normally the Auth0 subject), never an email address. */
    private String authSubject;
    /** Convenience login name only; Steam secrets are deliberately never persisted. */
    private String steamUsername;
    private String steamId;
    private Boolean siteAdmin = false;
    private Boolean leagueAdmin = false;
    private Boolean registerLeague = false;
    private List<String> adminForLeagueSystems = new ArrayList<>();
    private Boolean siteEditor = false;
    private List<String> editorForLeagueSystems = new ArrayList<>();

    /** Presentation preference only. Ratings are generated independently of this setting. */
    private Boolean showAiPlayerRatings = true;

    public AccountType effectiveAccountType() {
        return accountType == null ? AccountType.HUMAN : accountType;
    }
}
