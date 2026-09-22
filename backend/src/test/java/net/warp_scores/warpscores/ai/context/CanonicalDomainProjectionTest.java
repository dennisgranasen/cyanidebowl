package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalDomainProjectionTest {
    @Test
    void matchProjectionContainsSportingFactsWithoutMechanicalFields() {
        Match match = new Match(new SimpleIdentity("doc-1", 3));
        match.setMatchId("game-1");
        match.setCompetitionName("Nuffle Spitfire Trophy");
        match.setRound("Final");
        match.setStadium("The Pit");

        Team home = new Team(new SimpleIdentity("home", 3));
        home.setName("Råttfällan");
        home.setScore(2);
        home.setWinningsDice(6);
        Team away = new Team(new SimpleIdentity("away", 3));
        away.setName("Nottingham");
        away.setScore(1);
        away.setWinningsDice(1);
        match.setTeams(new Team[]{home, away});

        ContextItem item = new CanonicalContextMapper().match(
                match, ContextSource.DOMAIN, java.util.List.of());

        assertThat(item.body())
                .contains("Competition: Nuffle Spitfire Trophy.")
                .contains("Round: Final.")
                .contains("Result: Råttfällan 2–1 Nottingham.")
                .contains("Venue: The Pit.")
                .doesNotContain("Dice")
                .doesNotContain("roll")
                .doesNotContain("RNG")
                .doesNotContain("replay");
    }
}
