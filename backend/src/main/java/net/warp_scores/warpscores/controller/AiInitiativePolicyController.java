package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.domain.persistence.AiLeagueSystemInitiativePolicyRepository;
import net.warp_scores.warpscores.model.AiLeagueSystemInitiativePolicy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/admin/league-systems/{leagueSystemId}/ai-initiative-policy")
@RequiredArgsConstructor
public class AiInitiativePolicyController {
    private final AiLeagueSystemInitiativePolicyRepository policies;
    private final AiInitiativePolicyService resolver;

    @GetMapping
    @PreAuthorize("@userPermissionService.canEditLeagueSystem(authentication, #leagueSystemId)")
    public PolicyView get(@PathVariable String leagueSystemId) {
        return view(leagueSystemId);
    }

    @PutMapping
    @PreAuthorize("@userPermissionService.canEditLeagueSystem(authentication, #leagueSystemId)")
    public PolicyView update(
            Authentication authentication,
            @PathVariable String leagueSystemId,
            @RequestBody AiLeagueSystemInitiativePolicy update) {
        if (!StringUtils.hasText(leagueSystemId)) {
            throw new IllegalArgumentException("leagueSystemId is required");
        }
        if (update == null) {
            throw new IllegalArgumentException("policy is required");
        }

        validate(update);
        update.setLeagueSystemId(leagueSystemId);
        update.setUpdatedAt(Instant.now());
        update.setUpdatedBySubject(subject(authentication));
        policies.save(update);
        return view(leagueSystemId);
    }

    @DeleteMapping
    @PreAuthorize("@userPermissionService.canEditLeagueSystem(authentication, #leagueSystemId)")
    public PolicyView clear(@PathVariable String leagueSystemId) {
        policies.deleteById(leagueSystemId);
        return view(leagueSystemId);
    }

    private PolicyView view(String leagueSystemId) {
        AiLeagueSystemInitiativePolicy override =
                policies.findById(leagueSystemId).orElse(null);
        return new PolicyView(
                leagueSystemId,
                override,
                resolver.effective(leagueSystemId));
    }

    private static void validate(AiLeagueSystemInitiativePolicy policy) {
        var fans = policy.getFans();
        if (fans == null) return;
        validateProbability("generalArticleCommentProbability", fans.getGeneralArticleCommentProbability());
        validateProbability("ownTeamArticleCommentProbability", fans.getOwnTeamArticleCommentProbability());
        validateProbability("ownTeamMatchArticleCommentProbability", fans.getOwnTeamMatchArticleCommentProbability());
        validateProbability("ownTeamMatchCommentProbability", fans.getOwnTeamMatchCommentProbability());
        validateProbability("ownCoachActivityProbability", fans.getOwnCoachActivityProbability());
    }

    private static void validateProbability(String field, Double value) {
        if (value != null
                && (!Double.isFinite(value) || value < 0.0 || value > 1.0)) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }

    private static String subject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        return authentication == null ? null : authentication.getName();
    }

    public record PolicyView(
            String leagueSystemId,
            AiLeagueSystemInitiativePolicy override,
            AiInitiativePolicyService.EffectivePolicy effective) {
    }
}
