package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;

import org.springframework.beans.factory.annotation.Value;
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
    private final MatchService matchService;
    private final CyanideApiService cyanideApiService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @DurationLogging
    public List<Competition> loadForLeague(UUID leagueId, Optional<Integer> opus) {
        List<Competition> competitions = 
            competitionRepository.findByLeagueIdAndOpus(leagueId, opus.orElse(defaultOpus));
        if (competitions.isEmpty()) {
            competitions = cyanideApiService.loadCompetitions(leagueId, opus);
        }
        competitions.forEach(this::adjustCompetitionNameAndLogo);

        return competitions;
    }

    @DurationLogging
    public List<Competition> loadForLeague(Integer oldLeagueId, Optional<Integer> opus) {
    List<Competition> competitions = 
        competitionRepository.findByOldLeagueIdAndOpus(oldLeagueId, opus.orElse(defaultOpus));

        competitions.forEach(this::adjustCompetitionNameAndLogo);
        return competitions;
    }


    @DurationLogging
    public List<Competition> loadForLeagueAndInitialize(UUID leagueId, Optional<Integer> opus) {
        List<Competition> competitions = loadForLeague(leagueId, opus);
        return initializeForFormat(competitions);
    }

        @DurationLogging
    public List<Competition> loadForLeagueAndInitialize(Integer oldLeagueId, Optional<Integer> opus) {
        List<Competition> competitions = loadForLeague(oldLeagueId, opus);
        return initializeForFormat(competitions);
    }


    @DurationLogging
    public Optional<Competition> loadCompetition(UUID competitionId, Optional<Integer> opus) {
        Optional<Competition> competition = 
            competitionRepository.findByUuidAndOpus(competitionId, opus.orElse(defaultOpus))
                                 .map(this::initializeForFormat);
        competition.ifPresent(this::adjustCompetitionNameAndLogo);
        return competition;
    }

    @DurationLogging
    public Optional<Competition> loadCompetitionByOldId(int competitionId, Optional<Integer> opus) {
        log.info("Loading competition by old ID: {} with opus: {}", competitionId, opus.orElse(defaultOpus));

        Optional<Competition> competition = 
            competitionRepository.findByOldIdAndOpus(competitionId, opus.orElse(defaultOpus))
                                 .map(this::initializeForFormat);
        if (competition.isPresent()) {
            log.info("Competition {} found in DB, initializing for format...", competitionId);
            return competition;
        }
        log.info("Competition {} not found in DB, fetching from Cyanide API...", competitionId);
        List<net.warp_scores.warpscores.model.Competition> fetched = 
            cyanideApiService.loadCompetitions(competitionId, opus);
        // Optionally save to DB if found
        if (fetched != null) {
            log.info("Competitions {} fetched from Cyanide API, saving to DB...", competitionId);
            for (net.warp_scores.warpscores.model.Competition comp : fetched) {
                if (comp.getOldId() == competitionId) {
                    comp.setOldId(competitionId);
                    log.info("Saving competition: {}", comp);
                    competitionRepository.save(comp);
                    return Optional.of(comp);
                }
            }   
        }

        log.warn("Competition {} not found in Cyanide API either.", competitionId);
        return Optional.empty();
        

        //competition.ifPresent(this::adjustCompetitionNameAndLogo);
        //log.info("Loaded competition: {}", competition);
        //return competition;
    }

    @DurationLogging
    public Optional<Competition> loadCompetition(int oldId, Optional<Integer> opus) {
        Optional<Competition> competition;
        if (opus.orElse(defaultOpus) < 3) {
            log.info("Opus {} is less than 3, returning old competition.",
                opus.orElse(defaultOpus));            
            competition = competitionRepository.findByOldIdAndOpus(oldId, opus.orElse(defaultOpus));
            if (competition.isPresent()){
                Competition competitionObj = competition.get();
                initializeForFormat(competitionObj);
                adjustCompetitionNameAndLogo(competitionObj);
            }
        } else {
            log.error("Cannot load competition by int oldId when opus >= 3. Please provide a UUID.");
            competition = Optional.empty();
        }
        return competition;
    }

    private void adjustCompetitionNameAndLogo(Competition competition) {
        log.info("Adjusting competition name and logo for competition: {}", competition);
        officialLeagueCompetitions
                .adjustCompetitionNameAndLogo(
                    competition.getLeagueId(), 
                    competition.getName(),
                    competition::setName,
                    competition::setLogo);
    }

    private List<Competition> initializeForFormat(List<Competition> competitions) {
        return competitions.stream()
                .map(this::initializeForFormat)
                .collect(toList());
    }

    private Competition initializeForFormat(Competition competition) {
        if (CompetitionStatus.InProgress == competition.getStatus()) {
            switch (competition.getFormat()) {
                case RoundRobin -> initializeRoundRobin(competition);
                case Wissen -> initializeWissen(competition);
                case Knockout -> initializeKnockout(competition);
                case Ladder, Arena -> initializeLadder(competition);
                default -> notYetImplemented(competition.getFormat());
            }
        }
        return competition;
    }

    private void notYetImplemented(CompetitionFormat format) {
        log.error("CompetitionFormat '{}' not implemented yet.", format);
    }

    private void initializeRoundRobin(Competition competition) {
        Integer teams = competition.getTeamsMax();
        boolean isOdd = teams % 2 == 1;
        Integer contestCount = 
            contestsRepository.countByCompetitionId(competition.getUuid());
        List<Contest> contests = 
            contestsRepository.findByCompetitionId(
                competition.getUuid(), Optional.of(competition.getOpus()), Pageable.unpaged());
        Map<UUID, Optional<Contest>> uniqueContests = contests
                .stream()
                .collect(
                        groupingBy(
                                Contest::getContestUuid,
                                collectingAndThen(toList(), this::getLatest)));
        if (contests.size() != uniqueContests.size()) {
            log.info("Contests: {}, uniqueContests: {}.", contests.size(), uniqueContests.size());
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
                .stream().min(nullsFirst(comparing(Contest::getMatchDate).reversed()));
    }

    private void initializeWissen(Competition competition) {
        List<Contest> contests = 
            contestsRepository.findByCompetitionId(
                competition.getUuid(), Optional.of(competition.getOpus()), Pageable.unpaged());
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
        List<Contest> contests = contestsRepository.findByCompetitionId(
            competition.getUuid(), Optional.of(competition.getOpus()), Pageable.unpaged());
        competition.setCurrentRound(contests.stream().mapToInt(Contest::getRound).max().orElse(0));
        initializeMatchCount(competition, contests);
    }

    private void initializeLadder(Competition competition) {
        Integer matchCount = matchService.countByCompetitionId(competition.getUuid());
        competition.setTotalMatches(matchCount);
        competition.setPlayedMatches(matchCount);
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

    public Map<CompetitionStatus, Long> countForLeague(
            UUID leagueUuid, Optional<Integer> opus) {
        List<Competition> competitions = loadForLeagueAndInitialize(leagueUuid, opus);
        return competitions
                .stream()
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
