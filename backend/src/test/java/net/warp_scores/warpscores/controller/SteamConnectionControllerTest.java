package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.service.CoachClaimService;
import net.warp_scores.warpscores.service.ImageService;
import net.warp_scores.warpscores.service.PyBb3Client;
import net.warp_scores.warpscores.service.UserProfileService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SteamConnectionControllerTest {
    private ImageController imageController;

    @BeforeEach
    void setUp() {
        imageController = new ImageController(mock(CyanideApiProperties.class), mock(ImageService.class));
    }

    @Test
    void stripsOpusPrefixFromCanonicalBb3TeamId() {
        assertEquals("3e839d2f9b4c-11f1-a124-bc2411305479",
                SteamConnectionController.bb3TeamId("3_3e839d2f9b4c-11f1-a124-bc2411305479"));
    }

    @Test
    void leavesBareBb3TeamIdUnchanged() {
        assertEquals("team-1", SteamConnectionController.bb3TeamId("team-1"));
    }

    @Test
    void missingOrUnknownRaceImageReturnsNoContent() {
        assertEquals(HttpStatus.NO_CONTENT, imageController.getRaceImage("null", 3).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, imageController.getRaceImage("not-a-race", 3).getStatusCode());
    }

    @Test
    void nullFormationsResponseBecomesAnEmptyItemsList() {
        PyBb3Client pybb3 = mock(PyBb3Client.class);
        SteamConnectionController controller = new SteamConnectionController(
                pybb3, mock(UserProfileService.class), mock(CoachClaimService.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("BLASKSCORE_BB3_SESSION", "session-1") });
        when(auth.getName()).thenReturn("owner-1");
        when(pybb3.get("/api/v1/sessions/session-1/teams/team-1/formations", "owner-1")).thenReturn(null);

        Map<String, Object> result = controller.formations("team-1", auth, request);

        assertEquals(Map.of("items", java.util.List.of()), result);
    }
}