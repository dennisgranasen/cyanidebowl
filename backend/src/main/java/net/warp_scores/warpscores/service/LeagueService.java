package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.League;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;

    private final CompetitionService competitionService;

    public List<League> loadAll() {
        List<League> all = leagueRepository.findAll();
        all.forEach(this::countCompetitions);
        return all;
    }

    public Optional<League> loadById(UUID leagueUuid) {
        Optional<League> league = leagueRepository.findById(leagueUuid);
        league.ifPresent(this::countCompetitions);
        return league;
    }

    private void countCompetitions(League league) {
        Map<CompetitionStatus, Long> countsByStatus = competitionService.countForLeague(league.getUuid());
        league.setCountsByCompetitionStatus(countsByStatus);
    }
}
