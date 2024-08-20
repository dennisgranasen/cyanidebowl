package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {
    private final CompetitionRepository competitionRepository;
    private final ContestRepository contestsRepository;

    public List<Competition> loadForLeague(UUID leagueId) {
        List<Competition> competitions = competitionRepository.findByLeagueId(leagueId);
        return initializeForFormat(competitions);

    }

    public Optional<Competition> loadCompetition(UUID competitionId) {
        return competitionRepository.findById(competitionId)
                .map(this::initializeForFormat);
    }

    private List<Competition> initializeForFormat(List<Competition> competitions) {
        return competitions.stream()
                .map(this::initializeForFormat)
                .collect(toList());
    }

    private Competition initializeForFormat(Competition competition) {
        switch (competition.getFormat()) {
            case RoundRobin -> initializeRoundRobin(competition);
            case Wissen -> initializeWissen(competition);
            case Knockout -> initializeKnockout(competition);
            case Ladder -> initializeLadder(competition);
            default -> notYetImplemented(competition.getFormat());
        }
        return competition;
    }

    private void notYetImplemented(CompetitionFormat format) {
        log.error("CompetitionFormat '{}' not implemented yet.", format);
    }

    private void initializeRoundRobin(Competition competition) {
        Integer teams = competition.getTeamsMax();
        boolean isOdd = teams % 2 == 1;
        Integer contestCount = contestsRepository.countByCompetitionId(competition.getUuid());
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid());
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);
        int totalRounds = isOdd ? teams : teams - 1;
        int contestsPerRound = isOdd ? (teams - 1) / 2 : teams / 2;
        competition.setTotalRounds(totalRounds);
        competition.setCurrentRound(contestCount > 0 ? contestCount / contestsPerRound : 1);
        competition.setTotalMatches(totalRounds * contestsPerRound);
        competition.setPlayedMatches(playedMatchesCount);
        competition.setLiveMatches(liveMatches);
        Integer notValidatedCount = getNotValidatedMatchesCount(contests);
        competition.setNotValidatedMatches(notValidatedCount);
    }

    private void initializeWissen(Competition competition) {
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid());
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);

        OptionalInt currentRound = contests.stream().mapToInt(Contest::getRound).max();
        if (competition.getTotalRounds() == null) {
            competition.setTotalRounds(calcWissenTotalRounds(competition.getTeamsMax()));
        }
        competition.setCurrentRound(currentRound.orElse(0));
        competition.setPlayedMatches(playedMatchesCount);
        Integer notValidatedCount = getNotValidatedMatchesCount(contests);
        competition.setNotValidatedMatches(notValidatedCount);
        competition.setTotalMatches(competition.getTeamsMax() / 2 * competition.getTotalRounds());
        competition.setLiveMatches(liveMatches);
    }

    private void initializeKnockout(Competition competition) {
        Integer teams = competition.getTeamsMax();
        int totalRounds = 0;
        for (int players = 2; players <= teams; players *= 2) {
            totalRounds++;
        }
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid());
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);
        competition.setTotalRounds(totalRounds);
        competition.setPlayedMatches(playedMatchesCount);
        competition.setLiveMatches(liveMatches);
        Integer notValidatedCount = getNotValidatedMatchesCount(contests);
        competition.setNotValidatedMatches(notValidatedCount);
    }

    private void initializeLadder(Competition competition) {
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid());
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);
        competition.setPlayedMatches(playedMatchesCount);
        competition.setLiveMatches(liveMatches);
        Integer notValidatedCount = getNotValidatedMatchesCount(contests);
        competition.setNotValidatedMatches(notValidatedCount);
    }

    private static Integer getNotValidatedMatchesCount(List<Contest> contests) {
        Map<UUID, List<MatchStatus>> matchStatuses = contests
                .stream()
                .filter(contest -> Objects.nonNull(contest.getMatchUuid()))
                .collect(groupingBy(Contest::getMatchUuid,
                        mapping(Contest::getStatus, toList())));
        long notValidatedCount = matchStatuses
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().contains(MatchStatus.Validated))
                .count();
        return Long.valueOf(notValidatedCount).intValue();
    }

    public boolean competitionConsideredActive(Competition competition) {
        boolean inRegistrationOrInProgress = List.of(CompetitionStatus.Registration, CompetitionStatus.InProgress)
                .contains(competition.getStatus());
        boolean finished = CompetitionStatus.Finished.equals(competition.getStatus());
        int matchCount = Optional.ofNullable(competition.getPlayedMatches()).orElse(0);
        return inRegistrationOrInProgress || finished && matchCount > 0;
    }

    public Map<CompetitionStatus, Long> countForLeague(UUID leagueUuid) {
        List<Competition> competitions = loadForLeague(leagueUuid);
        return competitions
                .stream()
                .filter(this::competitionConsideredActive)
                .collect(
                        groupingBy(Competition::getStatus, Collectors.counting()));
    }

    public Integer calcWissenTotalRounds(Integer teamsMax) {
        int numTeams = teamsMax;
        if (teamsMax % 2 == 1) {
            numTeams += 1;
        }
        BigDecimal neededRounds = BigDecimal.valueOf(log2(numTeams));
        return neededRounds.setScale(0, RoundingMode.DOWN).intValue();
    }

    private static double log2(int x) {
        return (Math.log10(x) / Math.log10(2));
    }

}
