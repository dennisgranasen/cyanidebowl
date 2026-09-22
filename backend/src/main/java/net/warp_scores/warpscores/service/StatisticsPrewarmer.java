package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.scheduler.StatsScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsPrewarmer {
    private final StageSourceRepository stageSources;
    private final StatisticsService statistics;
    private final RankService ranks;
    private final StatsScheduler competitionStats;

    @Async
    public void prewarm(Set<String> seasons, Set<String> systems,
                        Identity competitionId, Identity leagueId) {
        for (StageSource source : stageSources.findAll()) {
            if (seasons.contains(source.getSeasonId()) && source.getLeagueSystemId() != null) {
                try {
                    statistics.season(source.getLeagueSystemId(), source.getSeasonId());
                } catch (RuntimeException e) {
                    log.warn("Could not precompute season statistics for {}: {}",
                            source.getSeasonId(), e.getMessage());
                }
            }
        }
        for (String systemId : systems) {
            try {
                statistics.marathon(systemId, "ALL", false, 0, 25, "points");
            } catch (RuntimeException e) {
                log.warn("Could not precompute marathon statistics for {}: {}", systemId, e.getMessage());
            }
        }
        if (competitionId != null) {
            try {
                ranks.getRanksForCompetition(competitionId, java.util.Optional.empty(), java.util.Optional.empty());
                competitionStats.rebuildCompetitionStats(competitionId);
            } catch (RuntimeException e) {
                log.warn("Could not precompute competition statistics for {}: {}", competitionId, e.getMessage());
            }
        }
        if (leagueId != null) {
            try {
                ranks.getRanksForLeague(leagueId, java.util.Optional.empty(), java.util.Optional.empty());
            } catch (RuntimeException e) {
                log.warn("Could not precompute league rankings for {}: {}", leagueId, e.getMessage());
            }
        }
    }
}