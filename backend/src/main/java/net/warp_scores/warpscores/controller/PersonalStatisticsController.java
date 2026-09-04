package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.WarpScoresUser;
import net.warp_scores.warpscores.service.StatisticsService;
import net.warp_scores.warpscores.service.UserProfileService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/user/statistics")
@RequiredArgsConstructor
public class PersonalStatisticsController {
    private final StatisticsService statistics;
    private final UserProfileService profiles;

    @GetMapping
    public StatisticsResponse.Personal get(@RequestParam String leagueSystemId, JwtAuthenticationToken auth) {
        WarpScoresUser user = profiles.getOrCreate(auth.getToken());
        List<String> coachIds = user.getCoachIds() == null ? List.of() : Arrays.asList(user.getCoachIds());
        return statistics.personal(leagueSystemId, coachIds);
    }
}
