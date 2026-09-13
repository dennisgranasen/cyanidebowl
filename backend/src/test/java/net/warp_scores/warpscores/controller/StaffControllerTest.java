package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.UserProfileService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StaffControllerTest {
    private final WarpScoresUserRepository users = mock(WarpScoresUserRepository.class);
    private final UserProfileService profiles = mock(UserProfileService.class);
    private final StaffController controller = new StaffController(users, profiles);

    @Test
    void listsOnlyEligibleHumanEditorsAndSortsByDisplayName() {
        WarpScoresUser siteEditor = human(1L, "z-login", "Zelda");
        siteEditor.setSiteEditor(true);

        WarpScoresUser scopedEditor = human(2L, "a-login", "Alice");
        scopedEditor.setEditorForLeagueSystems(List.of("nst"));

        WarpScoresUser ordinaryHuman = human(3L, "ordinary", "Ordinary");

        WarpScoresUser aiEditor = human(4L, "bot", "Bot");
        aiEditor.setAccountType(AccountType.AI);
        aiEditor.setSiteEditor(true);

        when(users.findAll()).thenReturn(List.of(siteEditor, ordinaryHuman, aiEditor, scopedEditor));

        var result = controller.list();

        assertThat(result)
                .extracting(StaffController.PublicProfile::displayName)
                .containsExactly("Alice", "Zelda");
        assertThat(result)
                .extracting(StaffController.PublicProfile::profileType)
                .containsOnly("HUMAN");
    }

    @Test
    void removingAllEditorialPermissionsMakesProfilePubliclyUnavailableWithoutDeletingUser() {
        WarpScoresUser user = human(42L, "editor", "Editor");
        user.setSiteEditor(false);
        user.setEditorForLeagueSystems(List.of());

        when(users.findById(42L)).thenReturn(Optional.of(user));

        var response = controller.get(42L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(users, never()).delete(any(WarpScoresUser.class));
        verify(users, never()).deleteById(anyLong());
    }

    @Test
    void developmentPrincipalDoesNotCreateOrUpdatePersistentStaffProfile() {
        var own = controller.own(null);
        var updated = controller.update(null,
                new StaffController.Update("Dev User", "avatar", "portrait", "bio"));

        assertThat(own.eligible()).isFalse();
        assertThat(own.id()).isNull();
        assertThat(updated.eligible()).isFalse();
        assertThat(updated.id()).isNull();
        verifyNoInteractions(profiles);
    }

    private static WarpScoresUser human(Long id, String username, String displayName) {
        WarpScoresUser user = new WarpScoresUser();
        user.setId(id);
        user.setUsername(username);
        user.setAccountType(AccountType.HUMAN);
        user.setPublicDisplayName(displayName);
        user.setStaffProfileInitialized(true);
        return user;
    }
}
