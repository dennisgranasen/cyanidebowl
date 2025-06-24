package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
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
    public List<Competition> loadForLeague(Identity leagueIdentity) {
        //List<Competition> competitions =
            //competitionRepository.findByLeagueId(leagueIdentity);
        //if (competitions.isEmpty()) {
            //competitions = cyanideApiService.loadCompetitions(leagueIdentity);
        //}
        List<Competition> competitions  = cyanideApiService.loadCompetitions(leagueIdentity)
            .stream()
            .map(competition -> {
                        //log.info("Competition {} for league {} @ opus:{}/{} fetched from Cyanide API, saving to DB...",
                        //    competition.getCompetitionId(), leagueIdentity.getValue(), competition.getIdentity().getOpus(), leagueIdentity.getOpus());
                        /*
                        if (competition.getLeagueId() == null) {
                            competition.setLeagueId(leagueIdentity.getValue());
                        }*/
                        adjustCompetitionNameAndLogo(competition);
                        competitionRepository.save(competition);
                        // here it could make sense to load the competition from the competiton endpoint as it has more details.
                        return competition;
                    })
            .toList();
                
        //competitions.forEach(this::adjustCompetitionNameAndLogo);

        return competitions;
    }

    @DurationLogging
    public List<Competition> loadForLeagueAndInitialize(Identity leagueIdentity) {
        log.info("Load/init competitions for league: {}", leagueIdentity.getValue());
        List<Competition> competitions = loadForLeague(leagueIdentity);
        return initializeForFormat(competitions);
    }

    @DurationLogging
    public Optional<Competition> loadCompetition(Identity competitionIdentity) {
        Optional<Competition> competition =
            competitionRepository.findById(competitionIdentity)
                                 .map(this::initializeForFormat);
        if (competition.isPresent()) {
            log.info("Competition {} found in DB, initializing for format...", competitionIdentity.getValue());
            adjustCompetitionNameAndLogo(competition.get());
        } else {
            log.info("Competition {} not found in DB, fetching from Cyanide API...", competitionIdentity.getValue());
            List<Competition> fetched =
                cyanideApiService.loadCompetitions(competitionIdentity);
            if (fetched != null && !fetched.isEmpty()) {
                log.info("Competitions {} fetched from Cyanide API, saving to DB...", competitionIdentity.getValue());
                for (Competition comp : fetched) {
                    log.info("Saving competition: {}", comp);
                    adjustCompetitionNameAndLogo(comp);
                    competitionRepository.save(comp);
                    return Optional.of(comp);
                }
            }
            log.warn("Competition {} not found in Cyanide API either.", competitionIdentity.getValue());
            return Optional.empty();
        }
        return competition;
    }

    private void adjustCompetitionNameAndLogo(Competition competition) {
        if (competition.getId().getOpus() > 2) {
            log.info("Adjusting competition name and logo for competition: {}", competition);
            officialLeagueCompetitions
                    .adjustCompetitionNameAndLogo(
                        competition.getLeagueId(),
                        competition.getName(),
                        competition::setName,
                        competition::setLogo);
        }
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
            contestsRepository.countByCompetitionId(competition.getId());
        List<Contest> contests =
            contestsRepository.findByCompetitionId(
                new SimpleIdentity(competition.getCompetitionId(), competition.getId().getOpus()),
                Pageable.unpaged());
        Map<Identity, Optional<Contest>> uniqueContests = contests
                .stream()
                .collect(
                        groupingBy(
                                Contest::getId,
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
            contestsRepository.findByCompetitionId(new SimpleIdentity(
                competition.getCompetitionId(), competition.getId().getOpus()),
                Pageable.unpaged());
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
            competition.getId(), Pageable.unpaged());
        competition.setCurrentRound(contests.stream().mapToInt(Contest::getRound).max().orElse(0));
        initializeMatchCount(competition, contests);
    }

    private void initializeLadder(Competition competition) {
        Integer matchCount = matchService.countByCompetitionId(competition.getId());
        if (matchCount == null) {
            competition.setTotalMatches(matchCount);
            competition.setPlayedMatches(matchCount);
        }
    }

    private void initializeMatchCount(Competition competition, List<Contest> contests) {
        Integer playedMatchesCount =
            contestsRepository.countByCompetitionIdAndMatchDateNotNull(
                competition.getId());
        Integer liveMatches =
            contestsRepository.countByCompetitionIdAndLive(
                competition.getId(), 1);
        Integer notPlayedAdministratedCount =
            getNotPlayedAdministratedMatchesCount(contests);
        Integer notValidatedCount = getNotValidatedMatchesCount(contests);
        competition.setPlayedMatches(playedMatchesCount + notPlayedAdministratedCount);
        competition.setLiveMatches(liveMatches);
        competition.setNotValidatedMatches(notValidatedCount);
    }

    private static Integer getNotValidatedMatchesCount(List<Contest> contests) {
        Map<Identity, List<MatchStatus>> matchStatuses = contests
                .stream()
                .filter(contest -> Objects.nonNull(contest.getMatchIdentity()))
                .collect(groupingBy(Contest::getMatchIdentity,
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
                .filter(contest -> Objects.isNull(contest.getMatchIdentity()))
                .filter(contest -> MatchStatus.Validated.equals(contest.getStatus()))
                .count();
        return Long.valueOf(count).intValue();
    }

    public Map<CompetitionStatus, Long> countForLeague(Identity leagueIdentity) {
        List<Competition> competitions = loadForLeagueAndInitialize(leagueIdentity);
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
