package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.UserPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static net.warp_scores.warpscores.controller.Permissions.READ_CURRENT_USER;
import static net.warp_scores.warpscores.controller.Permissions.WRITE_LEAGUE_ADMIN;
import static net.warp_scores.warpscores.controller.Permissions.WRITE_REGISTER_LEAGUE;
import static net.warp_scores.warpscores.controller.Permissions.WRITE_SITE_ADMIN;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @GetMapping(value = "/userPermissions")
    public ResponseEntity<UserPermissions> getUserPermissions(JwtAuthenticationToken principal) {
        List<String> permissions = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        UserPermissions userPermissions = new UserPermissions();
        userPermissions.setWriteRegisterLeague(permissions.contains(WRITE_REGISTER_LEAGUE));
        userPermissions.setReadCurrentUser(permissions.contains(READ_CURRENT_USER));
        userPermissions.setWriteSiteAdmin(permissions.contains(WRITE_SITE_ADMIN));
        userPermissions.setWriteLeagueAdmin(permissions.contains(WRITE_LEAGUE_ADMIN));
        return ResponseEntity.ok(userPermissions);
    }
}
