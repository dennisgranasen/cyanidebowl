package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiCommunityFanInteractionServiceTest {
    @Test
    void teamIndexUsesCanonicalTeamIdentity() {
        Match match = match("home-team", "away-team");
        String homeTeamId = match.getTeams()[0].getId().asMongoKey();
        String awayTeamId = match.getTeams()[1].getId().asMongoKey();
        String unrelatedTeamId = new SimpleIdentity("other-team", 3).asMongoKey();

        assertThat(AiCommunityFanInteractionService.teamIndex(match, homeTeamId)).isEqualTo(0);
        assertThat(AiCommunityFanInteractionService.teamIndex(match, awayTeamId)).isEqualTo(1);
        assertThat(AiCommunityFanInteractionService.teamIndex(match, unrelatedTeamId)).isEqualTo(-1);
    }

    @Test
    void homeCoachOnlyTriggersHomeTeamFans() {
        CommunityComment comment = new CommunityComment();
        comment.setAuthorContext(CommunityComment.AuthorContext.HOME_COACH);

        assertThat(AiCommunityFanInteractionService.authoredBySupportedCoach(comment, 0)).isTrue();
        assertThat(AiCommunityFanInteractionService.authoredBySupportedCoach(comment, 1)).isFalse();
    }

    @Test
    void awayCoachOnlyTriggersAwayTeamFans() {
        CommunityComment comment = new CommunityComment();
        comment.setAuthorContext(CommunityComment.AuthorContext.AWAY_COACH);

        assertThat(AiCommunityFanInteractionService.authoredBySupportedCoach(comment, 0)).isFalse();
        assertThat(AiCommunityFanInteractionService.authoredBySupportedCoach(comment, 1)).isTrue();
    }

    @Test
    void ordinaryUserIsNotTreatedAsSupportedCoach() {
        CommunityComment comment = new CommunityComment();
        comment.setAuthorContext(CommunityComment.AuthorContext.USER);

        assertThat(AiCommunityFanInteractionService.authoredBySupportedCoach(comment, 0)).isFalse();
        assertThat(AiCommunityFanInteractionService.authoredBySupportedCoach(comment, 1)).isFalse();
    }

    private static Match match(String homeId, String awayId) {
        Match match = new Match(new SimpleIdentity("match-id", 3));
        Team home = new Team(new SimpleIdentity(homeId, 3));
        Team away = new Team(new SimpleIdentity(awayId, 3));
        match.setTeams(new Team[] {home, away});
        return match;
    }
}
