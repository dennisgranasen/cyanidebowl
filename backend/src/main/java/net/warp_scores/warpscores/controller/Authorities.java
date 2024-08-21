package net.warp_scores.warpscores.controller;

import static net.warp_scores.warpscores.controller.Permissions.READ_CURRENT_USER;
import static net.warp_scores.warpscores.controller.Permissions.WRITE_LEAGUE_ADMIN;
import static net.warp_scores.warpscores.controller.Permissions.WRITE_REGISTER_LEAGUE;
import static net.warp_scores.warpscores.controller.Permissions.WRITE_SITE_ADMIN;

public interface Authorities {
    String AUTHORITY_READ_CURRENT_USER = "hasAuthority('" + READ_CURRENT_USER + "')";
    String AUTHORITY_WRITE_LEAGUE_ADMIN = "hasAuthority('" + WRITE_LEAGUE_ADMIN + "')";
    String AUTHORITY_WRITE_REGISTER_LEAGUE = "hasAuthority('" + WRITE_REGISTER_LEAGUE + "')";
    String AUTHORITY_WRITE_SITE_ADMIN = "hasAuthority('" + WRITE_SITE_ADMIN + "')";
}
