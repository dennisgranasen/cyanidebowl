package net.warp_scores.warpscores.model;

import lombok.Getter;

import java.util.List;

import static net.warp_scores.warpscores.model.Permissions.*;

@Getter
public class UserPermissions {
    private final boolean readCurrentUser;
    private final boolean writeLeagueAdmin;
    private final boolean writeSiteAdmin;
    private final boolean writeRegisterLeague;
    private final boolean globalLeagueAdmin;
    private final List<String> adminForLeagueSystems;
    private final boolean writeEditor;
    private final boolean globalEditor;
    private final List<String> editorForLeagueSystems;

    public UserPermissions(boolean readCurrentUser, boolean writeLeagueAdmin,
            boolean writeSiteAdmin, boolean writeRegisterLeague) {
        this(readCurrentUser, writeLeagueAdmin, writeSiteAdmin, writeRegisterLeague,
                writeLeagueAdmin || writeSiteAdmin, List.of(), writeSiteAdmin, writeSiteAdmin, List.of());
    }

    public UserPermissions(boolean readCurrentUser, boolean writeLeagueAdmin,
            boolean writeSiteAdmin, boolean writeRegisterLeague,
            boolean globalLeagueAdmin, List<String> adminForLeagueSystems) {
        this(readCurrentUser, writeLeagueAdmin, writeSiteAdmin, writeRegisterLeague,
                globalLeagueAdmin, adminForLeagueSystems, writeSiteAdmin, writeSiteAdmin, List.of());
    }

    public UserPermissions(boolean readCurrentUser, boolean writeLeagueAdmin,
            boolean writeSiteAdmin, boolean writeRegisterLeague,
            boolean globalLeagueAdmin, List<String> adminForLeagueSystems,
            boolean writeEditor, boolean globalEditor, List<String> editorForLeagueSystems) {
        this.readCurrentUser = readCurrentUser;
        this.writeLeagueAdmin = writeLeagueAdmin;
        this.writeSiteAdmin = writeSiteAdmin;
        this.writeRegisterLeague = writeRegisterLeague;
        this.globalLeagueAdmin = globalLeagueAdmin;
        this.adminForLeagueSystems = adminForLeagueSystems == null ? List.of() : List.copyOf(adminForLeagueSystems);
        this.writeEditor = writeEditor;
        this.globalEditor = globalEditor;
        this.editorForLeagueSystems = editorForLeagueSystems == null ? List.of() : List.copyOf(editorForLeagueSystems);
    }

    public static UserPermissions allPermissions() {
        return new UserPermissions(true, true, true, true, true, List.of(), true, true, List.of());
    }

    public static UserPermissions noPermissions() {
        return new UserPermissions(false, false, false, false, false, List.of(), false, false, List.of());
    }

    public UserPermissions withReadCurrentUser() {
        return copy(true, writeLeagueAdmin, writeSiteAdmin, writeRegisterLeague, globalLeagueAdmin,
                adminForLeagueSystems, writeEditor, globalEditor, editorForLeagueSystems);
    }

    public UserPermissions withWriteLeagueAdmin() {
        return copy(readCurrentUser, true, writeSiteAdmin, writeRegisterLeague, true,
                adminForLeagueSystems, writeEditor, globalEditor, editorForLeagueSystems);
    }

    public UserPermissions withWriteSiteAdmin() {
        return new UserPermissions(readCurrentUser, true, true, true, true,
                adminForLeagueSystems, true, true, editorForLeagueSystems);
    }

    public UserPermissions withWriteRegisterLeague() {
        return copy(readCurrentUser, writeLeagueAdmin, writeSiteAdmin, true, globalLeagueAdmin,
                adminForLeagueSystems, writeEditor, globalEditor, editorForLeagueSystems);
    }

    public UserPermissions withWriteEditor() {
        return copy(readCurrentUser, writeLeagueAdmin, writeSiteAdmin, writeRegisterLeague, globalLeagueAdmin,
                adminForLeagueSystems, true, true, editorForLeagueSystems);
    }

    public UserPermissions with(String permission) {
        return switch (permission) {
            case READ_CURRENT_USER -> withReadCurrentUser();
            case WRITE_REGISTER_LEAGUE -> withWriteRegisterLeague();
            case WRITE_LEAGUE_ADMIN -> withWriteLeagueAdmin();
            case WRITE_SITE_ADMIN -> withWriteSiteAdmin();
            case WRITE_EDITOR -> withWriteEditor();
            default -> this;
        };
    }

    private static UserPermissions copy(boolean readCurrentUser, boolean writeLeagueAdmin,
            boolean writeSiteAdmin, boolean writeRegisterLeague, boolean globalLeagueAdmin,
            List<String> adminForLeagueSystems, boolean writeEditor, boolean globalEditor,
            List<String> editorForLeagueSystems) {
        return new UserPermissions(readCurrentUser, writeLeagueAdmin, writeSiteAdmin, writeRegisterLeague,
                globalLeagueAdmin, adminForLeagueSystems, writeEditor, globalEditor, editorForLeagueSystems);
    }
}
