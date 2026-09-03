package net.warp_scores.warpscores.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.PyBb3Client;
import net.warp_scores.warpscores.service.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/user/steam")
@RequiredArgsConstructor
public class SteamConnectionController {
    private static final String COOKIE = "BLASKSCORE_BB3_SESSION";
    private final PyBb3Client pybb3;
    private final UserProfileService profiles;
    @Value("${pybb3.cookie-secure:true}") private boolean cookieSecure;

    @GetMapping
    public Map<String,Object> status(JwtAuthenticationToken auth, HttpServletRequest request) {
        WarpScoresUser user = profiles.getOrCreate(auth.getToken());
        String session = cookie(request);
        if (session != null) {
            try {
                Map<String,Object> current = pybb3.get("/api/v1/auth/sessions/" + session, auth.getName());
                var result = new java.util.HashMap<String,Object>();
                result.put("connected", true);
                result.put("steamUsername", current.get("steamUsername"));
                result.put("steamId", current.get("steamId"));
                return result;
            } catch (RuntimeException ignored) { /* expired cookie */ }
        }
        return Map.of("connected", false, "steamUsername", user.getSteamUsername() == null ? "" : user.getSteamUsername());
    }

    @PostMapping("/auth")
    public Map<String,Object> start(@RequestBody SteamLogin request, JwtAuthenticationToken auth, HttpServletResponse response) {
        Map<String,Object> result = pybb3.post("/api/v1/auth/start", auth.getName(), Map.of(
                "username", request.username(),
                "password", request.password()));
        complete(result, auth, response); return withoutSessionId(result);
    }

    @PostMapping("/challenges/{challengeId}/code")
    public Map<String,Object> code(@PathVariable String challengeId, @RequestBody GuardCode code,
                                   JwtAuthenticationToken auth, HttpServletResponse response) {
        Map<String,Object> result = pybb3.post("/api/v1/auth/challenges/" + challengeId + "/code", auth.getName(),
                Map.of("code", code.code()));
        complete(result, auth, response); return withoutSessionId(result);
    }

    @PostMapping("/challenges/{challengeId}/confirm")
    public Map<String,Object> confirm(@PathVariable String challengeId, JwtAuthenticationToken auth, HttpServletResponse response) {
        Map<String,Object> result = pybb3.post("/api/v1/auth/challenges/" + challengeId + "/confirm", auth.getName(), Map.of());
        complete(result, auth, response); return withoutSessionId(result);
    }

    @DeleteMapping
    public void logout(JwtAuthenticationToken auth, HttpServletRequest request, HttpServletResponse response) {
        String session = cookie(request);
        if (session != null) try { pybb3.delete("/api/v1/auth/sessions/" + session, auth.getName()); } catch (RuntimeException ignored) {}
        response.addHeader("Set-Cookie", cookieHeader("", Duration.ZERO).toString());
    }

    private void complete(Map<String,Object> result, JwtAuthenticationToken auth, HttpServletResponse response) {
        if (!"AUTHENTICATED".equals(result.get("status"))) return;
        profiles.connectSteam(auth.getToken(), (String)result.get("steamUsername"), (String)result.get("steamId"));
        response.addHeader("Set-Cookie", cookieHeader((String)result.get("sessionId"), Duration.ofMinutes(30)).toString());
    }
    private ResponseCookie cookieHeader(String value, Duration age) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(cookieSecure).sameSite("Strict").path("/").maxAge(age).build();
    }
    private String cookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    private Map<String,Object> withoutSessionId(Map<String,Object> source) {
        var copy = new java.util.HashMap<>(source); copy.remove("sessionId"); return copy;
    }
    public record SteamLogin(String username, String password) {}
    public record GuardCode(String code) {}
}
