package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPermissions {
    private boolean readCurrentUser;
    private boolean writeLeagueAdmin;
    private boolean writeSiteAdmin;
    private boolean writeRegisterLeague;
}
