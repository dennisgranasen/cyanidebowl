package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class AdminUserController {
    private final WarpScoresUserRepository users;

    @GetMapping
    public List<UserView> users() {
        return users.findAll().stream()
                .filter(user -> user.effectiveAccountType() == AccountType.HUMAN)
                .map(UserView::from)
                .sorted(Comparator.comparing(UserView::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @PutMapping("/{userId}/permissions")
    public ResponseEntity<UserView> permissions(@PathVariable Long userId,
            @RequestBody PermissionUpdate update) {
        return users.findById(userId)
                .filter(user -> user.effectiveAccountType() == AccountType.HUMAN)
                .map(user -> {
            user.setSiteAdmin(Boolean.TRUE.equals(update.siteAdmin()));
            user.setLeagueAdmin(Boolean.TRUE.equals(update.leagueAdmin()));
            user.setRegisterLeague(Boolean.TRUE.equals(update.registerLeague()));
            user.setAdminForLeagueSystems(update.adminForLeagueSystems() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(update.adminForLeagueSystems()));
            return ResponseEntity.ok(UserView.from(users.save(user)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PermissionUpdate(Boolean siteAdmin, Boolean leagueAdmin, Boolean registerLeague,
            List<String> adminForLeagueSystems) {}

    public record UserView(String id, String displayName, String username, String email, String provider,
            boolean siteAdmin, boolean leagueAdmin, boolean registerLeague,
            List<String> adminForLeagueSystems) {
        static UserView from(WarpScoresUser user) {
            String id = user.getId() == null ? "" : Long.toString(user.getId());
            String displayName = firstNonBlank(
                    user.getPublicDisplayName(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getAuthSubject(),
                    id);
            return new UserView(id, displayName, user.getUsername(), user.getEmail(), user.getProvider(),
                    Boolean.TRUE.equals(user.getSiteAdmin()),
                    Boolean.TRUE.equals(user.getLeagueAdmin()),
                    Boolean.TRUE.equals(user.getRegisterLeague()),
                    user.getAdminForLeagueSystems() == null ? List.of() : List.copyOf(user.getAdminForLeagueSystems()));
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) return value;
            }
            return "";
        }
    }
}
