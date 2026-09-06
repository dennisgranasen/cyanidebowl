package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.CoachClaim;
import net.warp_scores.warpscores.service.CoachClaimService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CoachClaimController {
    private final CoachClaimService coachClaims;

    @GetMapping("/user/coach-claims")
    public List<CoachClaim> mine(JwtAuthenticationToken auth) {
        return coachClaims.mine(auth.getToken());
    }

    @GetMapping("/user/coach-claims/candidates")
    public List<CoachClaimService.CoachCandidate> candidates(@RequestParam CoachClaim.Game game) {
        return coachClaims.candidates(game);
    }

    @PostMapping("/user/coach-claims")
    public List<CoachClaim> claim(@RequestBody ClaimRequest request, JwtAuthenticationToken auth) {
        return coachClaims.claimManual(auth.getToken(), request.game(), request.coachIds());
    }

    @DeleteMapping("/user/coach-claims/{id}")
    public void release(@PathVariable String id, JwtAuthenticationToken auth) {
        coachClaims.releaseMine(auth.getToken(), id);
    }

    @GetMapping("/admin/coach-claims")
    @PreAuthorize(Authorities.AUTHORITY_WRITE_SITE_ADMIN)
    public List<CoachClaim> all() {
        return coachClaims.all();
    }

    @DeleteMapping("/admin/coach-claims/{id}")
    @PreAuthorize(Authorities.AUTHORITY_WRITE_SITE_ADMIN)
    public void remove(@PathVariable String id) {
        coachClaims.adminRemove(id);
    }

    public record ClaimRequest(CoachClaim.Game game, List<String> coachIds) {}
}

