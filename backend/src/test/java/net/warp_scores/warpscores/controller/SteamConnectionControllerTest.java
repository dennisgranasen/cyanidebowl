package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.service.CoachClaimService;
import net.warp_scores.warpscores.service.ImageService;
import net.warp_scores.warpscores.service.PyBb3Client;
import net.warp_scores.warpscores.service.UserProfileService;
import net.warp_scores.warpscores.model.WarpScoresUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

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

    @Test
    void restoresRememberedSteamAndSetsANewSessionCookie() {
        PyBb3Client pybb3 = mock(PyBb3Client.class);
        UserProfileService profiles = mock(UserProfileService.class);
        CoachClaimService coachClaims = mock(CoachClaimService.class);
        SteamConnectionController controller = new SteamConnectionController(pybb3, profiles, coachClaims);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
        Jwt token = mock(Jwt.class);
        WarpScoresUser user = mock(WarpScoresUser.class);
        when(request.getCookies()).thenReturn(null);
        when(auth.getName()).thenReturn("owner-1");
        when(auth.getToken()).thenReturn(token);
        when(profiles.getOrCreate(token)).thenReturn(user);
        when(user.getSteamUsername()).thenReturn("steam-user");
        when(pybb3.get("/api/v1/auth/remembered", "owner-1")).thenReturn(
                Map.of("rememberAvailable", true, "remembered", true));
        when(pybb3.post("/api/v1/auth/restore", "owner-1", Map.of())).thenReturn(Map.of(
                "status", "AUTHENTICATED", "sessionId", "session-2",
                "steamUsername", "steam-user", "steamId", "7656119"));
        when(pybb3.get("/api/v1/sessions/session-2/teams?size=100&start=0", "owner-1"))
                .thenReturn(Map.of("items", List.of()));
        when(coachClaims.coachIds(token)).thenReturn(List.of("coach-1"));

        Map<String, Object> result = controller.status(auth, request, response);

        assertEquals(true, result.get("connected"));
        assertEquals(true, result.get("remembered"));
        assertEquals("steam-user", result.get("steamUsername"));
        assertFalse(result.containsKey("sessionId"));
        org.mockito.Mockito.verify(response).addHeader(eq("Set-Cookie"), contains("session-2"));
    }
}