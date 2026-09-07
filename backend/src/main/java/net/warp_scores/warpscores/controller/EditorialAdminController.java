package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.UserPermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/editor-grants")
public class EditorialAdminController {
    private final WarpScoresUserRepository users;
    private final UserPermissionService permissions;

    public record EditorGrantInput(boolean siteEditor, List<String> leagueSystemIds) {}

    @PutMapping("/{userId}")
    public WarpScoresUser setGrant(Authentication auth, @PathVariable Long userId,
                                   @RequestBody EditorGrantInput input) {
        if (!permissions.isSiteAdmin(auth)) {
            throw new AccessDeniedException("Site admin permission required");
        }
        WarpScoresUser user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setSiteEditor(input.siteEditor());
        user.setEditorForLeagueSystems(input.leagueSystemIds() == null
                ? new ArrayList<>() : new ArrayList<>(input.leagueSystemIds()));
        return users.save(user);
    }
}
