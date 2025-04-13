package net.warp_scores.warpscores.service;

import com.fasterxml.uuid.Generators;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.UUIDUtil;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;

@Service
@Slf4j
public class ContestInitializationService {

    private static final Team DUMMY_TEAM = new Team();

    static {
        DUMMY_TEAM.setName("Dummy Team");
        DUMMY_TEAM.setId(Generators.timeBasedGenerator().generate());
    }

    public List<Contest> initializeContestsScheduleForFormat(Optional<Competition> competition, List<Team> teams,
            List<Contest> contests) {
        return initializeContestsScheduleForFormat(competition, teams, contests, true);
    }

    @DurationLogging
    public List<Contest> initializeContestsScheduleForFormat(Optional<Competition> competition, List<Team> teams,
            List<Contest> contests, boolean generateFutureRoundRobinRounds) {

        Optional<CompetitionFormat> competitionFormat = competition.map(Competition::getFormat);
        if (teams.isEmpty() || competitionFormat.isEmpty() || competitionNotStarted(competition)) {
            return contests;
        }

        return switch (competitionFormat.get()) {
            case RoundRobin -> generateFutureRoundRobinRounds ?
                    initializeRoundRobinContests(unmodifiableList(contests), competition, teams) :
                    contests;
            case Knockout -> initializeKnockoutContests(unmodifiableList(contests), teams);
            case Wissen, Ladder, Arena -> contests;
        };
    }

    private boolean competitionNotStarted(Optional<Competition> competition) {
        return competition.map(value -> CompetitionStatus.Registration.equals(value.getStatus())).orElse(true);
    }

    @SuppressWarnings("rawtypes")
    private List<Contest> initializeKnockoutContests(List<Contest> contests,
            List<Team> teams) {
        int teamCount = teams.size();
        int totalRounds = 1;
        int players = 2;
        for (; players < teamCount; players *= 2) {
            totalRounds++;
        }
        int byes = players - teamCount;
        int totalMatches = Integer.max(teamCount - 1 - byes, contests.size());
        List<Contest> initializedContests = new ArrayList<>(contests);
        initializedContests.addAll(createEmptyFutureContests(contests, totalMatches, totalRounds));

        initializedContests.sort((contest1, contest2) -> {
            int compareResult = Integer.compare(contest1.getRound(), contest2.getRound());
            if (compareResult != 0) {
                return compareResult;
            }
            Instant instant1 = UUIDUtil.getInstantFromUUID(contest1.getContestUuid());
            Instant instant2 = UUIDUtil.getInstantFromUUID(contest2.getContestUuid());
            return instant1.compareTo(instant2);
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
                if (nextContest != null && !MatchStatus.Calculated.equals(nextContest.getStatus())) {
                    nextContest = findNextContestByWinner(currContest, currRound, initializedContests);
                } else {
                    if (currContest.getWinner() != null && nextContest != null) {
                        List<Team> opponents = nextContest.getOpponents();
                        opponents.add(createTeamFor((Map) currContest.getWinner()));
                        nextContest.setOpponents(opponents);
                    }
                }
                if (nextContest != null) {
                    currContest.setNextContestUuid(nextContest.getContestUuid());
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
        Optional<UUID> winnerTeamUuid = getWinnerTeamUuidFrom(currContest);
        return initializedContests
                .stream()
                .filter(contest -> contest.getRound().equals(currRound + 2))
                .filter(contest -> contest.getOpponents().stream().map(Team::getId)
                        .anyMatch(id -> winnerTeamUuid.isPresent() && winnerTeamUuid.get().equals(id)))
                .findFirst().orElse(null);
    }

    private static Optional<UUID> getWinnerTeamUuidFrom(Contest contest) {
        if (contest == null || contest.getWinner() == null) {
            return Optional.empty();
        }
        @SuppressWarnings("rawtypes")
        Map team = (Map) ((Map) contest.getWinner()).get("team");
        String winnerTeamUuidValue = (String) team.get("id");
        UUID winnerTeamUuid = UUID.fromString(winnerTeamUuidValue);
        return Optional.of(winnerTeamUuid);
    }

    @SuppressWarnings("rawtypes")
    private Team createTeamFor(Map winner) {
        Map teamMap = (Map) winner.get("team");
        Map coachMap = (Map) winner.get("coach");
        Team team = new Team();
        team.setId(UUID.fromString((String) teamMap.get("id")));
        team.setName((String) teamMap.get("name"));
        team.setCoachName((String) coachMap.get("name"));
        team.setLogo((String) teamMap.get("logo"));
        team.setRace(Race.forValue(teamMap.get("race")));
        return team;
    }

    private List<Contest> createEmptyFutureContests(List<Contest> contests, int totalMatches, int totalRounds) {
        int currRound = contests.stream().map(Contest::getRound).max(Integer::compareTo).orElse(0);
        int currRoundSize = contests.stream().filter(contest -> contest.getRound() == currRound).toList().size();
        List<Contest> futureContests = new ArrayList<>();
        if (contests.size() < totalMatches) {
            createNextRounds(futureContests, currRound + 1, currRoundSize / 2, totalRounds);
        }
        if (contests.size() + futureContests.size() != totalMatches) {
            throw new IllegalStateException(
                    String.format("Something went wrong (contests: %s, futureContests: %s, totalMatches: %s)...",
                            contests.size(), futureContests.size(), totalMatches));
        }
        return futureContests;
    }

    private void createNextRounds(List<Contest> contests, int currRound, int currRoundSize, int totalRounds) {
        if (currRound > totalRounds) {
            return;
        }
        IntStream.range(0, currRoundSize)
                .forEach(index -> contests.add(newContest(currRound)));
        createNextRounds(contests, currRound + 1, currRoundSize / 2, totalRounds);
    }

    private Contest newContest(int round) {
        Contest contest = new Contest();
        contest.setRound(round);
        contest.setContestUuid(Generators.timeBasedGenerator().generate());
        contest.setOpponents(new ArrayList<>());
        contest.setStatus(MatchStatus.Calculated);
        return contest;
    }

    private List<Contest> initializeRoundRobinContests(List<Contest> contests,
            Optional<Competition> competition,
            List<Team> teams) {
        OptionalInt currentRound = contests
                .stream()
                .mapToInt(Contest::getRound)
                .max();

        List<Team> homeTeams = new ArrayList<>();
        List<Team> awayTeams = new ArrayList<>();
        extractFirstRoundTeams(contests, homeTeams, awayTeams);
        addDummyTeamIfOddParticipants(teams, homeTeams, awayTeams);

        List<Contest> scheduledContests = competition.map(comp ->
                        generateScheduledContests(comp, homeTeams, awayTeams)
                                .stream()
                                .filter(this::doesNotContainDummyTeam)
                                .filter(contest -> contest.getRound() > currentRound.orElse(0))
                                .toList())
                .orElse(emptyList());

        List<Contest> initializedContests = new ArrayList<>(contests);
        initializedContests.addAll(scheduledContests);
        return initializedContests;
    }

    private boolean doesNotContainDummyTeam(Contest contest) {
        return !contest.getOpponents().contains(DUMMY_TEAM);
    }

    private void extractFirstRoundTeams(List<Contest> contests,
            List<Team> homeTeams,
            List<Team> awayTeams) {
        contests
                .stream()
                .filter(c -> c.getRound() == 1)
                .sorted(comparing(Contest::getContestUuid))
                .forEach(c -> {
                    homeTeams.add(c.getOpponents().get(0));
                    awayTeams.add(c.getOpponents().get(1));
                });
    }

    private void addDummyTeamIfOddParticipants
            (List<Team> teams, List<Team> homeTeams, List<Team> awayTeams) {
        boolean isEven = teams.size() % 2 == 0;
        if (isEven) {
            return;
        }

        Optional<Team> byeTeam = teams
                .stream()
                .filter(team -> !homeTeams.contains(team))
                .filter(team -> !awayTeams.contains(team))
                .findFirst();

        byeTeam.ifPresent(team -> {
            homeTeams.add(0, byeTeam.get());
            awayTeams.add(0, DUMMY_TEAM);
        });
    }

    private Collection<Contest> generateScheduledContests(Competition competition,
            List<Team> groupA,
            List<Team> groupB) {
        int participants = groupA.size() + groupB.size();
        List<Contest> scheduledContests = new ArrayList<>(getRound(competition, 0, groupA, groupB));
        for (int i = 1; i < participants - 1; i++) {
            groupB.add(0, groupA.remove(1));
            groupA.add(groupB.remove(groupB.size() - 1));
            scheduledContests.addAll(getRound(competition, i, groupA, groupB));
        }
        scheduledContests.sort(Contest::compareTo);
        return scheduledContests;
    }

    private static List<Contest> getRound(Competition competition, int round, List<
            Team> groupA, List<Team> groupB) {
        List<Contest> roundContests = new ArrayList<>();
        for (int i = 0; i < groupA.size(); i++) {
            Contest contest = new Contest();
            contest.setRound(round + 1);
            contest.setCompetitionId(competition.getUuid());
            contest.setCompetitionName(competition.getName());
            contest.setLeagueId(competition.getLeagueId());
            contest.setLeagueName(competition.getLeagueName());
            contest.setStatus(MatchStatus.Calculated);
            contest.setContestUuid(Generators.timeBasedGenerator().generate());
            contest.setOpponents(List.of(groupA.get(i), groupB.get(i)));
            roundContests.add(contest);
        }
        return roundContests;
    }
}
