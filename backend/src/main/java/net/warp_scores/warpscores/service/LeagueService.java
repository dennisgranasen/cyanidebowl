package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.League;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;

    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public List<League> loadAll() {
        return leagueRepository.findAll();
    }

    @DurationLogging
    public Optional<League> loadById(UUID leagueUuid) {
        return leagueRepository.findById(leagueUuid);
    }
}
