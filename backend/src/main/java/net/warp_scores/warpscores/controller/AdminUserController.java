package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
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
                .sorted(Comparator.comparing(user -> user.getUsername() == null ? "" : user.getUsername(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(UserView::from)
                .toList();
    }

    @PutMapping("/{userId}/permissions")
    public ResponseEntity<UserView> permissions(@PathVariable Long userId,
            @RequestBody PermissionUpdate update) {
        return users.findById(userId).map(user -> {
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

    public record UserView(Long id, String username, String email, String provider,
            boolean siteAdmin, boolean leagueAdmin, boolean registerLeague,
            List<String> adminForLeagueSystems) {
        static UserView from(WarpScoresUser user) {
            return new UserView(user.getId(), user.getUsername(), user.getEmail(), user.getProvider(),
                    Boolean.TRUE.equals(user.getSiteAdmin()),
                    Boolean.TRUE.equals(user.getLeagueAdmin()),
                    Boolean.TRUE.equals(user.getRegisterLeague()),
                    user.getAdminForLeagueSystems() == null ? List.of() : List.copyOf(user.getAdminForLeagueSystems()));
        }
    }
}
