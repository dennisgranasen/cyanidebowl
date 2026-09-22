package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.CoachClaimService;
import net.warp_scores.warpscores.service.StatisticsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/statistics")
@RequiredArgsConstructor
public class PersonalStatisticsController {
    private final StatisticsService statistics;
    private final CoachClaimService coachClaims;

    @GetMapping
    public StatisticsResponse.Personal get(@RequestParam String leagueSystemId, JwtAuthenticationToken auth) {
        return statistics.personal(leagueSystemId, coachClaims.mine(auth.getToken()));
    }
}
