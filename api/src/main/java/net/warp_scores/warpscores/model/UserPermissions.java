package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.warp_scores.warpscores.model.Permissions.READ_CURRENT_USER;
import static net.warp_scores.warpscores.model.Permissions.WRITE_LEAGUE_ADMIN;
import static net.warp_scores.warpscores.model.Permissions.WRITE_REGISTER_LEAGUE;
import static net.warp_scores.warpscores.model.Permissions.WRITE_SITE_ADMIN;

@Getter
@RequiredArgsConstructor
public class UserPermissions {
    private final boolean readCurrentUser;
    private final boolean writeLeagueAdmin;
    private final boolean writeSiteAdmin;
    private final boolean writeRegisterLeague;

    public static UserPermissions allPermissions() {
        return new UserPermissions(true, true, true, true);
    }

    public static UserPermissions noPermissions() {
        return new UserPermissions(false, false, false, false);
    }

    public UserPermissions withReadCurrentUser() {
        return new UserPermissions(true, this.writeLeagueAdmin, this.writeSiteAdmin, this.writeRegisterLeague);
    }

    public UserPermissions withWriteLeagueAdmin() {
        return new UserPermissions(this.readCurrentUser, true, this.writeSiteAdmin, this.writeRegisterLeague);
    }

    public UserPermissions withWriteSiteAdmin() {
        return new UserPermissions(this.readCurrentUser, this.writeLeagueAdmin, true, this.writeRegisterLeague);
    }

    public UserPermissions withWriteRegisterLeague() {
        return new UserPermissions(this.readCurrentUser, this.writeLeagueAdmin, this.writeSiteAdmin, true);
    }

    public UserPermissions with(String permission) {
        switch (permission) {
            case READ_CURRENT_USER:
                return this.withReadCurrentUser();
            case WRITE_REGISTER_LEAGUE:
                return this.withWriteRegisterLeague();
            case WRITE_LEAGUE_ADMIN:
                return this.withWriteLeagueAdmin();
            case WRITE_SITE_ADMIN:
                return this.withWriteSiteAdmin();
            default:
                return this;
        }
    }
}
