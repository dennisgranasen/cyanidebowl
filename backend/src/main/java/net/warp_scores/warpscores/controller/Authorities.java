package net.warp_scores.warpscores.controller;

import static net.warp_scores.warpscores.model.Permissions.READ_CURRENT_USER;

public interface Authorities {
    String AUTHORITY_READ_CURRENT_USER = "hasAuthority('" + READ_CURRENT_USER + "')";
    String AUTHORITY_WRITE_LEAGUE_ADMIN = "@userPermissionService.hasAnyLeagueAdmin(authentication)";
    String AUTHORITY_WRITE_REGISTER_LEAGUE = "@userPermissionService.canRegisterLeague(authentication)";
    String AUTHORITY_WRITE_SITE_ADMIN = "@userPermissionService.isSiteAdmin(authentication)";
}
