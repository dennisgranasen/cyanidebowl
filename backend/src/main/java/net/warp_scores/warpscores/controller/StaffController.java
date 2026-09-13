package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StaffController {
    private final WarpScoresUserRepository users;
    private final UserProfileService profiles;

    @GetMapping("/staff/users")
    public List<PublicProfile> list() {
        return users.findAll().stream().filter(StaffController::eligible).map(PublicProfile::from)
                .sorted(Comparator.comparing(PublicProfile::displayName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @GetMapping("/staff/users/{id}")
    public ResponseEntity<PublicProfile> get(@PathVariable Long id) {
        return users.findById(id).filter(StaffController::eligible).map(PublicProfile::from)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/staff-profile")
    public EditorProfile own(JwtAuthenticationToken principal) {
        if (principal == null) return developmentProfile();
        return EditorProfile.from(profiles.getOrCreate(principal.getToken()));
    }

    @PutMapping("/user/staff-profile")
    public EditorProfile update(JwtAuthenticationToken principal, @RequestBody Update update) {
        if (principal == null) return developmentProfile();
        try {
            return EditorProfile.from(profiles.updateStaffProfile(
                    principal.getToken(), update.displayName(), update.avatarUrl(),
                    update.portraitUrl(), update.bio()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private static boolean eligible(WarpScoresUser user) {
        return user.effectiveAccountType() == AccountType.HUMAN && (Boolean.TRUE.equals(user.getSiteEditor())
                || (user.getEditorForLeagueSystems() != null && !user.getEditorForLeagueSystems().isEmpty()));
    }

    public record PublicProfile(Long id, String profileType, String displayName, String avatarUrl, String portraitUrl, String bio) {
        static PublicProfile from(WarpScoresUser user) { return new PublicProfile(user.getId(), "HUMAN", StaffController.displayName(user), user.getPublicAvatarUrl(), user.getPublicPortraitUrl(), user.getPublicBio()); }
    }
    public record EditorProfile(Long id, boolean eligible, String displayName, String avatarUrl, String portraitUrl, String bio) {
        static EditorProfile from(WarpScoresUser user) { return new EditorProfile(user.getId(), StaffController.eligible(user), StaffController.displayName(user), user.getPublicAvatarUrl(), user.getPublicPortraitUrl(), user.getPublicBio()); }
    }
    public record Update(String displayName, String avatarUrl, String portraitUrl, String bio) {}

    private static EditorProfile developmentProfile() {
        return new EditorProfile(null, false, null, null, null, null);
    }

    private static String displayName(WarpScoresUser user) {
        return user.getPublicDisplayName() != null && !user.getPublicDisplayName().isBlank()
                ? user.getPublicDisplayName()
                : user.getUsername();
    }
}
