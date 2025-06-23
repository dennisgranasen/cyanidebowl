package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final CyanideApiService cyanideApiService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public List<League> loadAll() {
        return leagueRepository.findAll();
    }

    @DurationLogging
    public Optional<League> loadById(Identity leagueId) {        
        Optional<League> league = leagueRepository.findById(leagueId);
        log.info("Loading league by ID: {}, opus: {}", leagueId, leagueId.getOpus()); 
        if (league.isPresent()) {
            return league;
        } else {
            // Try to fetch from Cyanide API
            log.info("League {} not found in DB, fetching from Cyanide API...", leagueId);
            net.warp_scores.warpscores.model.League fetched = 
                cyanideApiService.loadLeague(leagueId);
            // Optionally save to DB if found
            if (fetched != null) {
                log.info("League {} fetched from Cyanide API, saving to DB...", leagueId);
                leagueRepository.save(fetched);
                return Optional.of(fetched);
            } else {
                log.warn("League {} not found in Cyanide API either.", leagueId);
                return Optional.empty();
            }
        }
    }

    /*
    public Optional<League> loadByOldId(Integer id, Optional<Integer> opus) {
        Optional<League> league = leagueRepository.findByOldIdAndOpus(id, 
            opus.orElse(defaultOpus)); // Default opus if not provided
        log.info("Loading league by old ID: {}, opus: {}", id, opus.orElse(defaultOpus));
        if (league.isPresent()) {
            log.info("League found in DB.", league.get());
            return league;
        } else {
            // Try to fetch from Cyanide API
            log.info("League {} not found in DB, fetching from Cyanide API...", id);
            net.warp_scores.warpscores.model.League fetched = cyanideApiService.loadOldLeague(id, opus);
            // Optionally save to DB if found
            if (fetched != null) {
                log.info("League {} fetched from Cyanide API, saving to DB...", id);
                leagueRepository.save(fetched);
                return Optional.of(fetched);
            } else {
                log.warn("League {} not found in Cyanide API either.", id);
                return Optional.empty();
            }
        }
    }
    */
}
