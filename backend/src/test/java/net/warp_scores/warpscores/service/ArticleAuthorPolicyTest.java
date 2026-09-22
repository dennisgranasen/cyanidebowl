package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticleAuthorPolicyTest {
    private final CoachClaimRepository claims = mock(CoachClaimRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final ArticleAuthorPolicy policy = new ArticleAuthorPolicy(claims, teams);
    private final Team team = new Team(new SimpleIdentity("team", 3));
    private final Player player = new Player(new SimpleIdentity("player", 3));
    @BeforeEach void setup() {
        CoachClaim claim = new CoachClaim(); claim.setGame(CoachClaim.Game.BB3); claim.setCoachId("coach");
        when(claims.findByAuthSubjectOrderByGameAscCoachNameAsc("human")).thenReturn(List.of(claim));
        team.setCoachId(new SimpleIdentity("coach", 3)); team.setPlayers(new Player[]{player});
        when(teams.findById(team.getId())).thenReturn(Optional.of(team));
        when(teams.findByPlayerId(player.getId())).thenReturn(List.of(team));
    }
    private Article.Association link(Article.LinkType type, String id) { return new Article.Association(type, id); }
    private boolean allowed(Article.Association... links) { return policy.ownsEntireAudience("human", List.of(links), List.of("news")); }
    @Test void ownTeamsAndCurrentPlayersCanPublishTogether() {
        assertTrue(allowed(link(Article.LinkType.TEAM, "3_team"), link(Article.LinkType.PLAYER, "3_player")));
        assertTrue(allowed(link(Article.LinkType.PLAYER, "3_player")));
    }
    @Test void everyAdditionalFeedRequiresReview() {
        for (Article.LinkType type : List.of(Article.LinkType.LEAGUE_SYSTEM, Article.LinkType.SEASON, Article.LinkType.FAN, Article.LinkType.STAFF)) {
            assertFalse(allowed(link(Article.LinkType.TEAM, "3_team"), link(type, "other")));
        }
        assertFalse(allowed(link(Article.LinkType.TEAM, "3_team"), link(Article.LinkType.TEAM, "3_other")));
        assertFalse(allowed());
        assertFalse(policy.ownsEntireAudience("human", List.of(link(Article.LinkType.TEAM, "3_team")), List.of("news", "other")));
    }
    @Test void sameCoachIdInAnotherGameIsNotOwnership() {
        team.setCoachId(new SimpleIdentity("coach", 2));
        assertFalse(allowed(link(Article.LinkType.TEAM, "3_team")));
    }
    @Test void transfersDeletedPlayersAndUnknownIdentitiesRequireReview() {
        team.setPlayers(new Player[0]);
        assertFalse(allowed(link(Article.LinkType.PLAYER, "3_player")));
        team.setPlayers(new Player[]{player}); player.setIsDeleted(true);
        assertFalse(allowed(link(Article.LinkType.PLAYER, "3_player")));
        assertFalse(allowed(link(Article.LinkType.PLAYER, "unknown")));
        team.setCoachId(new SimpleIdentity("another-coach", 3));
        assertFalse(allowed(link(Article.LinkType.TEAM, "3_team")));
    }
}
