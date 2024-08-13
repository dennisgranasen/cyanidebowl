package net.warp_scores.warpscores.service;

import com.fasterxml.uuid.Generators;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Comparator.comparing;

@Service
public class ContestInitializationService {

    private static final Team DUMMY_TEAM = new Team();

    static {
        DUMMY_TEAM.setName("Dummy Team");
        DUMMY_TEAM.setId(Generators.timeBasedGenerator().generate());
    }

    public List<Contest> initializeContestsScheduleForFormat(Optional<Competition> competition, List<Team> teams,
            List<Contest> contests) {

        List<Contest> initializedContests = new ArrayList<>(contests);

        Optional<CompetitionFormat> competitionFormat = competition.map(Competition::getFormat);
        if (teams.isEmpty() || !CompetitionFormat.RoundRobin.equals(competitionFormat.orElse(null))) {
            return initializedContests;
        }

        OptionalInt currentRound = contests
                .stream()
                .mapToInt(Contest::getRound)
                .max();

        List<Team> homeTeams = new ArrayList<>();
        List<Team> awayTeams = new ArrayList<>();
        extractFirstRoundTeams(contests, homeTeams, awayTeams);
        addDummyTeamIfOddParticipants(teams, homeTeams, awayTeams);

        List<Contest> scheduledContests = generateScheduledContests(competition.get(), homeTeams, awayTeams)
                .stream()
                .filter(this::doesNotContainDummyTeam)
                .filter(c -> c.getRound() > currentRound.orElse(0))
                .toList();

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

    private void addDummyTeamIfOddParticipants(List<Team> teams, List<Team> homeTeams, List<Team> awayTeams) {
        boolean isEven = teams.size() % 2 == 0;
        if (isEven) {
            return;
        }

        Optional<Team> byeTeam = teams
                .stream()
                .filter(team -> !homeTeams.contains(team))
                .filter(team -> !awayTeams.contains(team))
                .findFirst();

        homeTeams.add(0, byeTeam.get());
        awayTeams.add(0, DUMMY_TEAM);
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

    private static List<Contest> getRound(Competition competition, int round, List<Team> groupA, List<Team> groupB) {
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
