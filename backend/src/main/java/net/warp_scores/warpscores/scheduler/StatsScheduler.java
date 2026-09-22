package net.warp_scores.warpscores.scheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.warp_scores.warpscores.scheduler.Schedules.THIRTY_MINUTES;
import static net.warp_scores.warpscores.scheduler.Schedules.TWENTY_SECONDS;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.CompetitionStatsDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.CompetitionStatsRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStats;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.TeamAndRaceStats;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.MatchService;
import net.warp_scores.warpscores.service.StatsService;

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
        List<Identity> allCompetitionIds = competitionRepository
            .findAll()
            .stream()
            .map(Competition::getId)
            .toList();
        Map<Identity, Optional<Date>> lastMatchDatesForCompetitions = matchDomainService.getLastMatchDatesForCompetitions(
            allCompetitionIds);
        Map<Identity, Optional<Date>> lastUpdatedDatesForCompetitions = competitionStatsDomainService.getLastUpdatedDatesForCompetitions(
            allCompetitionIds);

        updateCompetitionStatsFor(allCompetitionIds, lastMatchDatesForCompetitions, lastUpdatedDatesForCompetitions);
    }

    private void updateCompetitionStatsFor(List<Identity> allCompetitionIds,
            Map<Identity, Optional<Date>> lastMatchDatesForCompetitions,
            Map<Identity, Optional<Date>> lastUpdatedDatesForCompetitions) {

        allCompetitionIds.forEach(competitionId -> {
            updateCompetitionStatsFor(competitionId,
                    lastMatchDatesForCompetitions.getOrDefault(competitionId, Optional.empty()),
                    lastUpdatedDatesForCompetitions.getOrDefault(competitionId, Optional.empty()));
        });

    }

    private void updateCompetitionStatsFor(Identity competitionId, Optional<Date> lastMatchDate, Optional<Date> lastUpdatedDate) {
        if ( lastMatchDate.isEmpty() )
        {
            log.debug("No match date yet for competition id {} skipping stats creation.", competitionId);
            return;
        }
        if ( lastUpdatedDate.isEmpty() || lastUpdatedDate.get().before(lastMatchDate.get()) )
        {
            log.info("Last match in competition {} was {}, last update of stats was {}.", competitionId, lastMatchDate, lastUpdatedDate);
            List<Match> matches = matchService.findByCompetitionId(competitionId);
            TeamAndRaceStats teamAndRaceStats = statsService.collectStats(matches);

            CompetitionStats competitionStats = new CompetitionStats();
            competitionStats.setCompetitionId(competitionId);
            competitionStats.setTeamAndRaceStats(teamAndRaceStats);
            competitionStats.setLastUpdated(lastMatchDate.get());

            competitionStatsRepository.save(competitionStats);
        } else {
            log.debug("Competition {} already up to date.", competitionId);
        }
    }
}
