package net.warp_scores.warpscores.service;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.Team;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.uuid.Generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;



@Service
@Slf4j
public class ContestInitializationService {

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    private final Team DUMMY_TEAM = new Team(
        new SimpleIdentity(Generators.timeBasedGenerator().generate(), 
                           defaultOpus))
    {
        @Override
        public String getName() {
            return "Dummy Team";
        }

        @Override
        public String getLogo() {
            return "https://cdn.warp-scores.net/teams/dummy-team.png";
        }
    };

    public List<Contest> initializeContestsScheduleForFormat(Competition competition, Collection<Team> teams,
            List<Contest> contests) {
        return initializeContestsScheduleForFormat(competition, teams, contests, true);
    }

    @DurationLogging
    public List<Contest> initializeContestsScheduleForFormat(Competition competition, Collection<Team> teams,
            List<Contest> contests, boolean generateFutureRoundRobinRounds) {

        CompetitionFormat competitionFormat = competition.getFormat();
        if (teams.isEmpty() || competitionNotStarted(competition)) {
            return contests;
        }

        /* updated with BB2 competition formats */
        return switch (competitionFormat) {
            case undefined -> emptyList();
            case RoundRobin, round_robin -> generateFutureRoundRobinRounds ?
                    initializeRoundRobinContests(unmodifiableList(contests), competition, teams) :
                    contests;
            case Knockout, single_elimination -> initializeKnockoutContests(unmodifiableList(contests), teams);
            case Wissen, Ladder, Arena, swiss, ladder -> contests;
        };
    }

    private boolean competitionNotStarted(Competition competition) {
        return CompetitionStatus.Registration.equals(competition.getStatus());
    }

    @SuppressWarnings("rawtypes")
    private List<Contest> initializeKnockoutContests(List<Contest> contests,
            Collection<Team> teams) {
        int teamCount = teams.size();
        int totalRounds = 1;
        int players = 2;
        for (; players < teamCount; players *= 2) {
            totalRounds++;
        }
        int byes = players - teamCount;
        int totalMatches = Integer.max(teamCount - 1 - byes, contests.size());
        List<Contest> initializedContests = new ArrayList<>(contests);
        initializedContests.addAll(createEmptyFutureContests(
            contests, totalMatches, totalRounds, 
            contests.isEmpty() ? defaultOpus : contests.get(0).getId().getOpus() 
        ));

        initializedContests.sort((contest1, contest2) -> {
            int compareResult = Integer.compare(contest1.getRound(), contest2.getRound());
            if (compareResult != 0) {
                return compareResult;
            }
            return contest1.compareTo(contest2);
        });

        int[] roundMatches = new int[totalRounds];
        roundMatches[0] = teamCount / 2;
        for (int i = 1; i < totalRounds; i++) {
            roundMatches[i] = roundMatches[i - 1] / 2;
        }
        int currRoundOffset = 0;
        for (int currRound = 0; currRound < totalRounds; currRound++) {
            int nextRoundOffset = currRoundOffset + roundMatches[currRound];
            for (int matchIndex = currRoundOffset; matchIndex < nextRoundOffset; matchIndex++) {
                Contest currContest = initializedContests.get(matchIndex);
                int nextMatchIndexWithinRound = (int) Math.floor((double) (matchIndex - currRoundOffset) / 2);
                Contest nextContest = findNextContestByIndex(nextRoundOffset,
                        nextMatchIndexWithinRound, initializedContests);
                int opus = currContest.getId().getOpus();
                if (nextContest != null && !MatchStatus.Calculated.equals(nextContest.getStatus())) {
                    nextContest = findNextContestByWinner(currContest, currRound, initializedContests);
                } else {
                    if (currContest.getWinner() != null && nextContest != null) {
                        Team[] opponents = nextContest.getOpponents();
                        Team newOpponent = createTeamFor((Map) currContest.getWinner(), opus);
                        opponents = Arrays.copyOf(opponents, opponents.length + 1);
                        opponents[opponents.length - 1] = newOpponent;
                        nextContest.setOpponents(opponents);
                    }
                }
                if (nextContest != null) {
                    currContest.setNextContestId(nextContest.getContestId());
                }
            }
            currRoundOffset = nextRoundOffset;
        }
        return initializedContests;
    }

    private static Contest findNextContestByIndex(int nextRoundOffset,
            int nextMatchIndexWithinRound,
            List<Contest> initializedContests) {
        return nextRoundOffset + nextMatchIndexWithinRound < initializedContests.size() ? initializedContests.get(
                nextRoundOffset + nextMatchIndexWithinRound) : null;
    }

    private static Contest findNextContestByWinner(Contest currContest,
            int currRound,
            List<Contest> initializedContests) {
        log.info(currContest.getId() + " -" + currContest.getWinner());
        Optional<Identity> winnerTeamId = getWinnerTeamIdFrom(currContest);
        return initializedContests
                .stream()
                .filter(contest -> contest.getRound().equals(currRound + 2))
                .filter(contest -> Arrays.stream(contest.getOpponents()).map(Team::getId)
                        .anyMatch(id -> winnerTeamId.isPresent() && winnerTeamId.get().equals(id)))
                .findFirst().orElse(null);
    }

    private static Optional<Identity> getWinnerTeamIdFrom(Contest contest) {
        if (contest == null || contest.getWinner() == null) {
            return Optional.empty();
        }
        @SuppressWarnings("rawtypes")
        Map team = (Map) ((Map) contest.getWinner()).get("team");

        Object winnerTeamId = (Object) team.get("id");
        return Optional.of(new SimpleIdentity(winnerTeamId, contest.getId().getOpus()));
    }

    @SuppressWarnings("rawtypes")
    private Team createTeamFor(Map winner, int opus) {
        Map teamMap = (Map) winner.get("team");
        Map coachMap = (Map) winner.get("coach");
        SimpleIdentity teamId = new SimpleIdentity(
            (String) teamMap.get("id"), 
            opus);
        Team team = new Team(teamId);
        team.setName((String) teamMap.get("name"));
        team.setCoachName((String) coachMap.get("name"));
        team.setLogo((String) teamMap.get("logo"));
        team.setRace((String) teamMap.get("race"));
        return team;
    }

    private List<Contest> createEmptyFutureContests(
            List<Contest> contests, int totalMatches, int totalRounds, int opus) {
        int currRound = contests.stream().map(Contest::getRound).max(Integer::compareTo).orElse(0);
        int currRoundSize = contests.stream().filter(contest -> contest.getRound() == currRound).toList().size();
        List<Contest> futureContests = new ArrayList<>();
        if (contests.size() < totalMatches) {
            createNextRounds(futureContests, currRound + 1, currRoundSize / 2, totalRounds, opus);
        }
        if (contests.size() + futureContests.size() != totalMatches) {
            throw new IllegalStateException(
                    String.format("Something went wrong (contests: %s, futureContests: %s, totalMatches: %s)...",
                            contests.size(), futureContests.size(), totalMatches));
        }
        return futureContests;
    }

    private void createNextRounds(List<Contest> contests, 
            int currRound, int currRoundSize, int totalRounds, int opus) {
        if (currRound > totalRounds) {
            return;
        }
        IntStream.range(0, currRoundSize)
                .forEach(index -> contests.add(newContest(currRound, opus)));
        createNextRounds(contests, currRound + 1, currRoundSize / 2, totalRounds, opus);
    }

    private Contest newContest(int round, int opus) {
        SimpleIdentity identity = new SimpleIdentity(
                Generators.timeBasedGenerator().generate(), opus);
        Contest contest = new Contest(identity);
        contest.setRound(round);
        contest.setOpponents(new Team[0]);
        contest.setStatus(MatchStatus.Calculated);
        return contest;
    }

    private List<Contest> initializeRoundRobinContests(List<Contest> contests,
            Competition competition,
            Collection<Team> teams) {
        log.info("DEBUG: Starting initializeRoundRobinContests");
        OptionalInt currentRound = contests
                .stream()
                .mapToInt(Contest::getRound)
                .max();

        log.info("DEBUG: Current round: {}", currentRound.orElse(0));

        List<Contest> scheduledContests = 
            generateScheduledContests(competition, teams)
                    .stream()
                    .filter(this::doesNotContainDummyTeam)
                    .filter(contest -> contest.getRound() > currentRound.orElse(0))
                    .toList();                

        log.info("DEBUG: Scheduled contests: {}", scheduledContests.size());
        /*
        for (Contest c : scheduledContests) {
            log.info("DEBUG: Scheduled Contest round={}, home={}, away={}", c.getRound(), c.getOpponents()[0].getName(), c.getOpponents()[1].getName());
        }
        */
        List<Contest> initializedContests = new ArrayList<>(contests);
        initializedContests.addAll(scheduledContests);

        log.info("DEBUG: Initialized contests: {}", initializedContests.size());
/*
        for (Contest c : initializedContests) {
            log.info("DEBUG: Initialized Contest round={}, home={}, away={}", c.getRound(), c.getOpponents()[0].getName(), c.getOpponents()[1].getName());
        }
*/
        return initializedContests;
    }

    private boolean doesNotContainDummyTeam(Contest contest) {
        return !Arrays.stream(contest.getOpponents()).anyMatch(x -> x.equals(DUMMY_TEAM));
    }

    private Collection<Contest> generateScheduledContests(Competition competition, Collection<Team> teams) {
        int n = teams.size();
        boolean isOdd = n % 2 != 0;
        Team dummy = null;
        List<Team> workingTeams = new ArrayList<>(teams);
        if (isOdd) {
            dummy = new Team(new SimpleIdentity("DUMMY", competition.getId().getOpus()));
            workingTeams.add(dummy);
            n++;
        }

        int rounds = n - 1;
        int half = n / 2;
        List<Contest> scheduledContests = new ArrayList<>();
        int opus = competition.getId().getOpus();

        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < half; i++) {
                Team t1 = workingTeams.get(i);
                Team t2 = workingTeams.get(n - 1 - i);
                if (t1 == dummy || t2 == dummy) continue;

                Team home, away;
                if (i == 0 && round % 2 == 0 && round > 0) {
                    // Swap home/away for the first pairing every other round (Berger rule)
                    home = t2;
                    away = t1;
                } else {
                    home = t1;
                    away = t2;
                }

                Contest contest = new Contest(new SimpleIdentity(
                    Generators.timeBasedGenerator().generate(), opus));
                contest.setRound(round + 1);
                contest.setCompetitionId(competition.getId());
                contest.setCompetitionName(competition.getName());
                contest.setLeagueId(competition.getLeagueId());
                contest.setLeagueName(competition.getLeagueName());
                contest.setStatus(MatchStatus.Calculated);                
                contest.setOpponents(new Team[]{home, away});
                scheduledContests.add(contest);
            }
            // Rotate teams except the first one
            List<Team> newOrder = new ArrayList<>();
            newOrder.add(workingTeams.get(0));
            newOrder.add(workingTeams.get(n - 1));
            newOrder.addAll(workingTeams.subList(1, n - 1));
            workingTeams = newOrder;
        }

        scheduledContests.forEach(contest -> {
            if (contest.getRound()>1) {
                contest.setRound(2 + rounds - contest.getRound());
            }
        });

        scheduledContests.sort(Contest::compareTo);
        return scheduledContests;
    }
}
