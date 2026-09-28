package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.Auth0ManagementUserService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AdminUserControllerTest {
    private final WarpScoresUserRepository users = mock(WarpScoresUserRepository.class);
        private final Auth0ManagementUserService auth0Users = mock(Auth0ManagementUserService.class);
        private final AdminUserController controller = new AdminUserController(users, auth0Users);

    @Test
    void listsOnlyHumanUsersWithStableStringIdsAndReadableDisplayNames() {
        WarpScoresUser human = new WarpScoresUser();
        human.setId(9007199254740993L);
        human.setUsername("oauth-login");
        human.setPublicDisplayName("Readable Name");

        WarpScoresUser legacyHuman = new WarpScoresUser();
        legacyHuman.setId(42L);
        legacyHuman.setEmail("legacy@example.com");

        WarpScoresUser ai = new WarpScoresUser();
        ai.setId(7L);
        ai.setAccountType(AccountType.AI);
        ai.setUsername("robot");

        when(users.findAll()).thenReturn(List.of(ai, legacyHuman, human));

        var result = controller.users();

        assertThat(result).extracting(AdminUserController.UserView::displayName)
                .containsExactly("legacy@example.com", "Readable Name");
        assertThat(result).extracting(AdminUserController.UserView::id)
                .containsExactly("42", "9007199254740993");
    }

    @Test
    void refusesPermissionChangesForAiAccounts() {
        WarpScoresUser ai = new WarpScoresUser();
        ai.setId(7L);
        ai.setAccountType(AccountType.AI);
        when(users.findById(7L)).thenReturn(Optional.of(ai));

        var response = controller.permissions(7L, new AdminUserController.PermissionUpdate(
                true, true, true, List.of("nst")));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(users, never()).save(any());
    }

        @Test
        void enrichesMissingLocalIdentityFromAuth0() {
                WarpScoresUser user = new WarpScoresUser();
                user.setId(42L);
                user.setAuthSubject("auth0|user-42");
                user.setProvider("auth0");
                when(users.findAll()).thenReturn(List.of(user));
                when(auth0Users.findUser("auth0|user-42"))
                                .thenReturn(java.util.Optional.of(new Auth0ManagementUserService.Identity(
                                                "Alex Example", "alex@example.com")));

                var result = controller.users();

                assertThat(result).hasSize(1);
                assertThat(result.getFirst().displayName()).isEqualTo("Alex Example");
                assertThat(result.getFirst().email()).isEqualTo("alex@example.com");
        }

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
