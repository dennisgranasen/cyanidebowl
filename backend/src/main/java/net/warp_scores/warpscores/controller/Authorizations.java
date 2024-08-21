package net.warp_scores.warpscores.controller;

public interface Authorizations {
    String READ_CURRENT_USER = "hasAuthority('read:current_user')";
    String WRITE_LEAGUE_ADMIN = "hasAuthority('write:league_admin')";
    String WRITE_REGISTER_LEAGUE= "hasAuthority('write:register_league')";
    String WRITE_SITE_ADMIN = "hasAuthority('write:site_admin')";
}
