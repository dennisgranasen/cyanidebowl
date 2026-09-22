package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.CoachClaimRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CoachClaim;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Only exclusive references to the author's currently owned teams/players qualify. */
@Service
@RequiredArgsConstructor
public class ArticleAuthorPolicy {
    private final CoachClaimRepository claims;
    private final TeamRepository teams;

    public boolean ownsEntireAudience(String subject, List<Article.Association> links, List<String> channels) {
        if (subject == null || links.isEmpty()) return false;
        if (channels != null && channels.stream().anyMatch(channel -> !"news".equals(channel))) return false;
        if (links.stream().anyMatch(link -> link.type() != Article.LinkType.TEAM && link.type() != Article.LinkType.PLAYER)) return false;
        List<CoachClaim> ownedCoaches = claims.findByAuthSubjectOrderByGameAscCoachNameAsc(subject);
        if (ownedCoaches.isEmpty()) return false;
        return links.stream().allMatch(link -> {
            try {
                Identity identity = IdentityUtil.fromId(link.id());
                if (link.type() == Article.LinkType.TEAM) {
                    return teams.findById(identity).filter(team -> owned(team, ownedCoaches)).isPresent();
                }
                // Check current rosters, not historical participation in a team the coach once owned.
                List<Team> currentTeams = teams.findByPlayerId(identity);
                return !currentTeams.isEmpty() && currentTeams.stream().allMatch(team ->
                        owned(team, ownedCoaches) && team.getPlayers() != null && Arrays.stream(team.getPlayers())
                                .anyMatch(player -> player != null && !Boolean.TRUE.equals(player.getIsDeleted())
                                        && player.getId() != null && identity.asMongoKey().equals(player.getId().asMongoKey())));
            } catch (IllegalArgumentException ex) {
                return false;
            }
        });
    }

    private boolean owned(Team team, List<CoachClaim> claims) {
        if (team == null || Boolean.TRUE.equals(team.getIsDeleted()) || team.getCoachId() == null) return false;
        return claims.stream().anyMatch(claim -> claim.getGame() != null
                && claim.getGame().opus() == team.getCoachId().getOpus()
                && Objects.equals(claim.getCoachId(), team.getCoachId().getValue()));
    }
}
