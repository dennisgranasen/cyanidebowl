package net.warp_scores.warpscores.model;

import lombok.Getter;

import java.util.List;

import static net.warp_scores.warpscores.model.Permissions.READ_CURRENT_USER;
import static net.warp_scores.warpscores.model.Permissions.WRITE_LEAGUE_ADMIN;
import static net.warp_scores.warpscores.model.Permissions.WRITE_REGISTER_LEAGUE;
import static net.warp_scores.warpscores.model.Permissions.WRITE_SITE_ADMIN;

@Getter
public class UserPermissions {
    private final boolean readCurrentUser;
    private final boolean writeLeagueAdmin;
    private final boolean writeSiteAdmin;
    private final boolean writeRegisterLeague;
    private final boolean globalLeagueAdmin;
    private final List<String> adminForLeagueSystems;

    public UserPermissions(boolean readCurrentUser, boolean writeLeagueAdmin,
            boolean writeSiteAdmin, boolean writeRegisterLeague) {
        this(readCurrentUser, writeLeagueAdmin, writeSiteAdmin, writeRegisterLeague,
                writeLeagueAdmin || writeSiteAdmin, List.of());
    }

    public UserPermissions(boolean readCurrentUser, boolean writeLeagueAdmin,
            boolean writeSiteAdmin, boolean writeRegisterLeague,
            boolean globalLeagueAdmin, List<String> adminForLeagueSystems) {
        this.readCurrentUser = readCurrentUser;
        this.writeLeagueAdmin = writeLeagueAdmin;
        this.writeSiteAdmin = writeSiteAdmin;
        this.writeRegisterLeague = writeRegisterLeague;
        this.globalLeagueAdmin = globalLeagueAdmin;
        this.adminForLeagueSystems = adminForLeagueSystems == null ? List.of() : List.copyOf(adminForLeagueSystems);
    }

    public static UserPermissions allPermissions() {
        return new UserPermissions(true, true, true, true, true, List.of());
    }

    public static UserPermissions noPermissions() {
        return new UserPermissions(false, false, false, false, false, List.of());
    }

    public UserPermissions withReadCurrentUser() {
        return new UserPermissions(true, this.writeLeagueAdmin, this.writeSiteAdmin, this.writeRegisterLeague,
                this.globalLeagueAdmin, this.adminForLeagueSystems);
    }

    public UserPermissions withWriteLeagueAdmin() {
        return new UserPermissions(this.readCurrentUser, true, this.writeSiteAdmin, this.writeRegisterLeague,
                true, this.adminForLeagueSystems);
    }

    public UserPermissions withWriteSiteAdmin() {
        return new UserPermissions(this.readCurrentUser, true, true, true, true, this.adminForLeagueSystems);
    }

    public UserPermissions withWriteRegisterLeague() {
        return new UserPermissions(this.readCurrentUser, this.writeLeagueAdmin, this.writeSiteAdmin, true,
                this.globalLeagueAdmin, this.adminForLeagueSystems);
    }

    public UserPermissions with(String permission) {
        return switch (permission) {
            case READ_CURRENT_USER -> this.withReadCurrentUser();
            case WRITE_REGISTER_LEAGUE -> this.withWriteRegisterLeague();
            case WRITE_LEAGUE_ADMIN -> this.withWriteLeagueAdmin();
            case WRITE_SITE_ADMIN -> this.withWriteSiteAdmin();
            default -> this;
        };
    }
}
