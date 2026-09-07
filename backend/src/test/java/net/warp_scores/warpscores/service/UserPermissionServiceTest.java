package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static net.warp_scores.warpscores.model.Permissions.WRITE_LEAGUE_ADMIN;
import static net.warp_scores.warpscores.model.Permissions.WRITE_SITE_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPermissionServiceTest {
    private final WarpScoresUserRepository users = mock(WarpScoresUserRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final PhaseRepository phases = mock(PhaseRepository.class);
    private final StageRepository stages = mock(StageRepository.class);
    private final StageSourceRepository stageSources = mock(StageSourceRepository.class);
    private final RegisteredSourceRepository registeredSources = mock(RegisteredSourceRepository.class);

    private final UserPermissionService service = new UserPermissionService(
            users, seasons, phases, stages, stageSources, registeredSources);

    @Test
    void siteAdminStoredOnUserSupersedesLeagueSystemAssignments() {
        WarpScoresUser user = user();
        user.setSiteAdmin(true);
        when(users.findByAuthSubject("auth0|dennis")).thenReturn(Optional.of(user));

        var permissions = service.permissions(jwt());

        assertThat(permissions.isWriteSiteAdmin()).isTrue();
        assertThat(permissions.isWriteLeagueAdmin()).isTrue();
        assertThat(permissions.isGlobalLeagueAdmin()).isTrue();
        assertThat(permissions.isWriteRegisterLeague()).isTrue();
        assertThat(service.canAdminLeagueSystem(jwt(), "any-system")).isTrue();
    }

    @Test
    void siteAdminAuthorityAlsoSupersedesDatabaseScopes() {
        when(users.findByAuthSubject("auth0|dennis")).thenReturn(Optional.empty());

        var auth = jwt(WRITE_SITE_ADMIN);

        assertThat(service.isSiteAdmin(auth)).isTrue();
        assertThat(service.canAdminLeagueSystem(auth, "any-system")).isTrue();
    }

    @Test
    void scopedLeagueAdminMayOnlyAdminAssignedLeagueSystems() {
        WarpScoresUser user = user();
        user.setAdminForLeagueSystems(List.of("nst"));
        when(users.findByAuthSubject("auth0|dennis")).thenReturn(Optional.of(user));

        var auth = jwt();

        assertThat(service.hasAnyLeagueAdmin(auth)).isTrue();
        assertThat(service.isGlobalLeagueAdmin(auth)).isFalse();
        assertThat(service.canAdminLeagueSystem(auth, "nst")).isTrue();
        assertThat(service.canAdminLeagueSystem(auth, "other")).isFalse();
    }

    @Test
    void globalLeagueAdminAuthorityMayAdminEveryLeagueSystemButIsNotSiteAdmin() {
        when(users.findByAuthSubject("auth0|dennis")).thenReturn(Optional.empty());

        var auth = jwt(WRITE_LEAGUE_ADMIN);

        assertThat(service.isGlobalLeagueAdmin(auth)).isTrue();
        assertThat(service.canAdminLeagueSystem(auth, "nst")).isTrue();
        assertThat(service.canAdminLeagueSystem(auth, "other")).isTrue();
        assertThat(service.isSiteAdmin(auth)).isFalse();
    }

    private WarpScoresUser user() {
        WarpScoresUser user = new WarpScoresUser();
        user.setAuthSubject("auth0|dennis");
        return user;
    }

    private JwtAuthenticationToken jwt(String... authorities) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("auth0|dennis")
                .build();
        return new JwtAuthenticationToken(jwt,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}
