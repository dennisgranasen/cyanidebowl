package net.warp_scores.warpscores.service;

import com.fasterxml.uuid.Generators;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ContestInitializationServiceTest {
    private final ContestInitializationService service = new ContestInitializationService();

    private Competition givenCompetition;
    private List<Contest> givenSeedContests;

    private List<Contest> initializedContests;

    @Test
    public void roundRobinCompetitionContestsAreGenerated() {
        givenCompetition(CompetitionFormat.RoundRobin);
        givenSeedContests(new String[]{"A", "F", "B", "E", "C", "D"});

        whenContestsInitialized();

        thenExpectRoundRobinGeneratedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"A", "B", "C", "F", "D", "E"},
                new String[]{"A", "C", "D", "B", "E", "F"},
                new String[]{"A", "D", "E", "C", "F", "B"},
                new String[]{"A", "E", "F", "D", "B", "C"}
        );
    }

    @Test
    public void generationForRoundRobinDoesNotOverrideGivenContests() {
        givenCompetition(CompetitionFormat.RoundRobin);
        givenSeedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"U", "V", "W", "X", "Y", "Z"},
                new String[]{"V", "W", "X", "Y", "Z", "U"}
        );

        whenContestsInitialized();

        thenExpectRoundRobinGeneratedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"U", "V", "W", "X", "Y", "Z"},
                new String[]{"V", "W", "X", "Y", "Z", "U"},
                new String[]{"A", "D", "E", "C", "F", "B"},
                new String[]{"A", "E", "F", "D", "B", "C"}
        );
    }

    @Test
    public void generationForWissenDoesNothing() {
        givenCompetition(CompetitionFormat.Wissen);
        givenSeedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"U", "V", "W", "X", "Y", "Z"},
                new String[]{"V", "W", "X", "Y", "Z", "U"}
        );

        whenContestsInitialized();

        thenExpectRoundRobinGeneratedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"U", "V", "W", "X", "Y", "Z"},
                new String[]{"V", "W", "X", "Y", "Z", "U"}
        );
    }

    @Test
    public void generationForKnockoutDoesNothing() {
        givenCompetition(CompetitionFormat.Knockout);
        givenSeedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"U", "V", "W", "X", "Y", "Z"},
                new String[]{"V", "W", "X", "Y", "Z", "U"}
        );

        whenContestsInitialized();

        thenExpectRoundRobinGeneratedContests(
                new String[]{"A", "F", "B", "E", "C", "D"},
                new String[]{"U", "V", "W", "X", "Y", "Z"},
                new String[]{"V", "W", "X", "Y", "Z", "U"}
        );
    }

    @Test
    public void serviceDoesNotChangeGivenContests() {
        givenCompetition(CompetitionFormat.RoundRobin);
        List<Contest> seedContests = createPairedContests(1, new String[]{"A", "F", "B", "E", "C", "D"});
        this.givenSeedContests = new ArrayList<>(seedContests);

        whenContestsInitialized();

        assertThat(this.givenSeedContests, is(seedContests));
    }

    private void thenExpectRoundRobinGeneratedContests(String[]... pairings) {
        int contestsCount = Arrays.stream(pairings).mapToInt(c -> c.length).sum() / 2;
        assertThat(this.initializedContests.size(), is(contestsCount));

        List<Contest> expectedContests = new ArrayList<>();
        for (int i = 0; i < pairings.length; i++) {
            List<Contest> pairedContests = createPairedContests(i + 1, pairings[i]);
            expectedContests.addAll(pairedContests);
        }

        assertThat(this.initializedContests.size(), is(expectedContests.size()));

        for (int i = 0; i < expectedContests.size(); i++) {
            Contest initializedContest = this.initializedContests.get(i);
            Contest expectedContest = expectedContests.get(i);
            assertThat(String.format("Contest#%s is same round (%s)", i + 1, expectedContest.getRound()),
                    initializedContest.getRound(), is(expectedContest.getRound()));
            String expectedHomeTeam = expectedContest.getOpponents().get(0).getName();
            String expectedAwayTeam = expectedContest.getOpponents().get(1).getName();

            assertThat(String.format("Home team for Contest#%s.%s to be %s", expectedContest.getRound(), (i % 3) + 1,
                            expectedHomeTeam),
                    initializedContest.getOpponents().get(0).getName(),
                    is(expectedHomeTeam));
            assertThat(String.format("Away team for Contest#%s.%s to be %s", expectedContest.getRound(), (i % 3) + 1,
                            expectedAwayTeam),
                    initializedContest.getOpponents().get(1).getName(),
                    is(expectedAwayTeam));
        }
    }

    private void givenCompetition(CompetitionFormat competitionFormat) {
        this.givenCompetition = new Competition();
        this.givenCompetition.setFormat(competitionFormat);
        this.givenCompetition.setName("Test Competition");
    }

    private void givenSeedContests(String[]... teamNames) {
        List<Contest> seedContests = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            seedContests.addAll(createPairedContests(i + 1, teamNames[i]));
        }
        givenSeedContests = Collections.unmodifiableList(seedContests);
    }

    private void whenContestsInitialized() {
        this.initializedContests = this.service.initializeContestsScheduleForFormat(Optional.of(givenCompetition),
                givenSeedContests);
    }

    private static Contest createContest(int round, String teamNameA, String teamNameB) {
        Contest contest = new Contest();
        contest.setContestUuid(Generators.timeBasedGenerator().generate());
        Team teamA = createTeam(teamNameA);
        Team teamB = createTeam(teamNameB);
        contest.setOpponents(Arrays.asList(teamA, teamB));
        contest.setRound(round);
        return contest;
    }

    private static Team createTeam(String teamName) {
        Team team = new Team();
        team.setName(teamName);
        return team;
    }

    private static List<Contest> createPairedContests(int round, String[] teamNames) {
        if (teamNames == null || teamNames.length == 0 || teamNames.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Team names must not be null, greater than zero and contain an even number of elements");
        }
        List<Contest> contests = new ArrayList<>(teamNames.length / 2);
        for (int i = 0; i < teamNames.length; i += 2) {
            contests.add(createContest(round, teamNames[i], teamNames[i + 1]));
        }
        return contests;
    }
}

