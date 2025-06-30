package net.warp_scores.warpscores.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.CompetitionStatsDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.CompetitionStatsRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStats;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.TeamAndRaceStats;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.MatchService;
import net.warp_scores.warpscores.service.StatsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.warp_scores.warpscores.scheduler.Schedules.THIRTY_MINUTES;
import static net.warp_scores.warpscores.scheduler.Schedules.TWENTY_SECONDS;

@Slf4j
@Service
public class StatsScheduler {

    private final CompetitionService competitionService;
    private final CompetitionRepository competitionRepository;
    private final StatsService statsService;
    private final MatchService matchService;
    private final MatchDomainService matchDomainService;
    private final CompetitionStatsDomainService competitionStatsDomainService;
    private final CompetitionStatsRepository competitionStatsRepository;

    public StatsScheduler(CompetitionService competitionService, CompetitionRepository competitionRepository,
            StatsService statsService, MatchService matchService, MatchDomainService matchDomainService,
            CompetitionStatsDomainService competitionStatsDomainService,
            CompetitionStatsRepository competitionStatsRepository) {
        this.competitionService = competitionService;
        this.competitionRepository = competitionRepository;
        this.statsService = statsService;
        this.matchService = matchService;
        this.matchDomainService = matchDomainService;
        this.competitionStatsDomainService = competitionStatsDomainService;
        this.competitionStatsRepository = competitionStatsRepository;
    }

    @Scheduled(initialDelay = TWENTY_SECONDS, fixedDelay = THIRTY_MINUTES)
    public void updateCompetitionStats() {
        List<UUID> allCompetitionUuids = competitionRepository
                .findAll()
                .stream()
                .map(Competition::getUuid)
                .toList();
        Map<UUID, Optional<Date>> lastMatchDatesForCompetitions = matchDomainService.getLastMatchDatesForCompetitions(
                allCompetitionUuids);
        Map<UUID, Optional<Date>> lastUpdatedDatesForCompetitions = competitionStatsDomainService.getLastUpdatedDatesForCompetitions(
                allCompetitionUuids);

        updateCompetitionStatsFor(allCompetitionUuids, lastMatchDatesForCompetitions, lastUpdatedDatesForCompetitions);
    }

    private void updateCompetitionStatsFor(List<UUID> allCompetitionUuids,
            Map<UUID, Optional<Date>> lastMatchDatesForCompetitions,
            Map<UUID, Optional<Date>> lastUpdatedDatesForCompetitions) {

        allCompetitionUuids.forEach(competitionUuid -> {
            updateCompetitionStatsFor(competitionUuid,
                    lastMatchDatesForCompetitions.getOrDefault(competitionUuid, Optional.empty()),
                    lastUpdatedDatesForCompetitions.getOrDefault(competitionUuid, Optional.empty()));
        });

    }

    private void updateCompetitionStatsFor(UUID competitionUuid, Optional<Date> lastMatchDate, Optional<Date> lastUpdatedDate) {
        if ( lastMatchDate.isEmpty() )
        {
            log.info("No match date yet for competition id {} skipping stats creation.", competitionUuid);
            return;
        }
        if ( lastUpdatedDate.isEmpty() || lastUpdatedDate.get().before(lastMatchDate.get()) )
        {
            log.info("Last match in competition {} was {}, last update of stats was {}.", competitionUuid, lastMatchDate, lastUpdatedDate);
            List<Match> matches = matchService.findByCompetitionId(competitionUuid);
            TeamAndRaceStats teamAndRaceStats = statsService.collectStats(matches);

            CompetitionStats competitionStats = new CompetitionStats();
            competitionStats.setCompetitionUuid(competitionUuid);
            competitionStats.setTeamAndRaceStats(teamAndRaceStats);
            competitionStats.setLastUpdated(lastMatchDate.get());

            competitionStatsRepository.save(competitionStats);
        } else {
            log.info("No match date after last update date yet for competition id {} skipping stats creation.", competitionUuid);
        }
    }
}
