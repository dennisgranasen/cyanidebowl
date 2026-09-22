package net.warp_scores.warpscores.config;

import net.warp_scores.warpscores.service.UserPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LeagueSystemAdminInterceptorTest {
    @Test
    void deniedListRequestReturnsForbiddenInsteadOfEmptySuccess() throws Exception {
        var permissions = mock(UserPermissionService.class);
        var interceptor = new LeagueSystemAdminInterceptor(permissions, null, null, null, null, null);
        var request = new MockHttpServletRequest("GET", "/admin/league-systems");
        var response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void developmentTokenUsesCanonicalPermissions() {
        var token = new LocalSecurityConfiguration().jwtDecoder().decode("dev-token");
        assertThat(token.getClaimAsStringList("permissions")).contains(
                "write:site_admin", "write:league_admin", "write:register_league", "read:current_user");
    }
}
