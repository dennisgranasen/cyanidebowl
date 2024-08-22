package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.UserPermissions;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final Environment environment;

    @GetMapping(value = "/userPermissions")
    public ResponseEntity<UserPermissions> getUserPermissions(JwtAuthenticationToken principal) {
        UserPermissions userPermissions = getUserPermissionsFor(principal);
        return ResponseEntity.ok(userPermissions);
    }

    private UserPermissions getUserPermissionsFor(JwtAuthenticationToken principal) {
        if (principal == null) {
            List<String> activeProfiles = List.of(environment.getActiveProfiles());
            if (activeProfiles.contains("dev") && !activeProfiles.contains("prod")) {
                return UserPermissions.allPermissions();
            } else {
                return UserPermissions.noPermissions();
            }
        }

        List<String> permissions = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        UserPermissions userPermissions = UserPermissions.noPermissions();
        for (String permission : permissions) {
            userPermissions = userPermissions.with(permission);
        }
        return userPermissions;
    }
}
