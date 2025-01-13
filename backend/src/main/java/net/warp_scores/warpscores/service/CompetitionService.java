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
import org.springframework.data.domain.Pageable;
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

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {
    private final CompetitionRepository competitionRepository;
    private final ContestRepository contestsRepository;
    private final OfficialLeagueAndCompetitions officialLeagueCompetitions;

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
            case Ladder, Arena -> initializeLadder(competition);
            default -> notYetImplemented(competition.getFormat());
        }
        List.of(competition)
                .forEach(c -> officialLeagueCompetitions.adjustCompetitionName(c.getLeagueId(), c.getName(), c::setName));
        return competition;
    }

    private void notYetImplemented(CompetitionFormat format) {
        log.error("CompetitionFormat '{}' not implemented yet.", format);
    }

    private void initializeRoundRobin(Competition competition) {
        Integer teams = competition.getTeamsMax();
        boolean isOdd = teams % 2 == 1;
        Integer contestCount = contestsRepository.countByCompetitionId(competition.getUuid());
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid(), Pageable.unpaged());
        Map<UUID, Optional<Contest>> uniqueContests = contests
                .stream()
                .collect(
                        groupingBy(
                                Contest::getContestUuid,
                                collectingAndThen(toList(), this::getLatest)));
        if (contests.size() != uniqueContests.keySet().size()) {
            log.info("Contests: {}, uniqueContests: {}.", contests.size(), uniqueContests.keySet().size());
        }
        int totalRounds = isOdd ? teams : teams - 1;
        int contestsPerRound = isOdd ? (teams - 1) / 2 : teams / 2;
        competition.setTotalRounds(totalRounds);
        competition.setCurrentRound(contestCount > 0 ? contestCount / contestsPerRound : 1);
        competition.setTotalMatches(totalRounds * contestsPerRound);

        initializeMatchCount(competition, contests);
    }

    private Optional<Contest> getLatest(List<Contest> contests) {
        return contests
                .stream()
                .sorted(nullsFirst(comparing(Contest::getMatchDate).reversed()))
                .findFirst();
    }

    private void initializeWissen(Competition competition) {
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid(), Pageable.unpaged());
        OptionalInt currentRound = contests.stream().mapToInt(Contest::getRound).max();
        if (competition.getTotalRounds() == null) {
            competition.setTotalRounds(calcWissenTotalRounds(competition.getTeamsMax()));
        }
        competition.setCurrentRound(currentRound.orElse(0));
        competition.setTotalMatches(competition.getTeamsMax() / 2 * competition.getTotalRounds());
        initializeMatchCount(competition, contests);
    }

    private void initializeKnockout(Competition competition) {
        Integer teams = competition.getTeamsMax();
        int totalRounds = 1;
        int players = 2;
        for (; players < teams; players *= 2) {
            totalRounds++;
        }
        competition.setTotalRounds(totalRounds);
        int byes = players - teams;
        int totalMatches = teams - 1 - byes;
        competition.setTotalMatches(totalMatches);
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid(), Pageable.unpaged());
        competition.setCurrentRound(contests.stream().mapToInt(Contest::getRound).max().orElse(0));
        initializeMatchCount(competition, contests);
    }

    private void initializeLadder(Competition competition) {
        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid(), Pageable.unpaged());
        initializeMatchCount(competition, contests);
    }

    private void initializeMatchCount(Competition competition, List<Contest> contests) {
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);
        Integer notPlayedAdministratedCount = getNotPlayedAdministratedMatchesCount(contests);
        Integer notValidatedCount = getNotValidatedMatchesCount(contests);
        competition.setPlayedMatches(playedMatchesCount + notPlayedAdministratedCount);
        competition.setLiveMatches(liveMatches);
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

    private static Integer getNotPlayedAdministratedMatchesCount(List<Contest> contests) {
        long count = contests
                .stream()
                .filter(contest -> Objects.isNull(contest.getMatchUuid()))
                .filter(contest -> MatchStatus.Validated.equals(contest.getStatus()))
                .count();
        return Long.valueOf(count).intValue();
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
