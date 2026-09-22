package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.UserPermissions;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.LocalizationService;
import net.warp_scores.warpscores.service.UserPermissionService;
import net.warp_scores.warpscores.service.UserProfileService;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final Environment environment;
    private final UserPermissionService permissions;
    private final UserProfileService profiles;
    private final LocalizationService localization;

    @GetMapping(value = "/userPermissions")
    public ResponseEntity<UserPermissions> getUserPermissions(JwtAuthenticationToken principal) {
        log.info("Got user {}...",
                Optional.ofNullable(principal).map(JwtAuthenticationToken::getName).orElse("<anonymous>"));

        if (principal == null) {
            List<String> activeProfiles = List.of(environment.getActiveProfiles());
            return ResponseEntity.ok(activeProfiles.contains("dev") && !activeProfiles.contains("prod")
                    ? UserPermissions.allPermissions()
                    : UserPermissions.noPermissions());
        }
        return ResponseEntity.ok(permissions.permissions(principal));
    }

    @GetMapping("/user/preferences")
    public ResponseEntity<UserPreferences> preferences(JwtAuthenticationToken principal) {
        WarpScoresUser user = profiles.getOrCreate(principal.getToken());
        return ResponseEntity.ok(new UserPreferences(user.getLocale()));
    }

    @PutMapping("/user/preferences")
    public ResponseEntity<UserPreferences> preferences(JwtAuthenticationToken principal,
            @RequestBody UserPreferences update) {
        try {
            String locale = localization.optionalSupported(update.locale());
            WarpScoresUser user = profiles.updateLocale(principal.getToken(), locale);
            return ResponseEntity.ok(new UserPreferences(user.getLocale()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record UserPreferences(String locale) {}
}
