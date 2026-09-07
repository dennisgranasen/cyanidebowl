package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AdminUserControllerTest {
    private final WarpScoresUserRepository users = mock(WarpScoresUserRepository.class);
    private final AdminUserController controller = new AdminUserController(users);

    @Test
    void updatesGlobalAndLeagueSystemPermissionsWithoutLegacyPermissionLists() {
        WarpScoresUser user = new WarpScoresUser();
        user.setId(42L);
        user.setUsername("coach");
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(users.save(any(WarpScoresUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.permissions(42L, new AdminUserController.PermissionUpdate(
                true, false, true, List.of("nst", "other")));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().siteAdmin()).isTrue();
        assertThat(response.getBody().leagueAdmin()).isFalse();
        assertThat(response.getBody().registerLeague()).isTrue();
        assertThat(response.getBody().adminForLeagueSystems()).containsExactly("nst", "other");
        verify(users).save(argThat(saved ->
                Boolean.TRUE.equals(saved.getSiteAdmin())
                        && !Boolean.TRUE.equals(saved.getLeagueAdmin())
                        && Boolean.TRUE.equals(saved.getRegisterLeague())
                        && saved.getAdminForLeagueSystems().equals(List.of("nst", "other"))));
    }

    @Test
    void replacingPermissionsRemovesOldLeagueSystemAssignments() {
        WarpScoresUser user = new WarpScoresUser();
        user.setId(42L);
        user.setAdminForLeagueSystems(List.of("old"));
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(users.save(any(WarpScoresUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.permissions(42L, new AdminUserController.PermissionUpdate(
                false, false, false, List.of("new")));

        assertThat(user.getAdminForLeagueSystems()).containsExactly("new");
    }
}
