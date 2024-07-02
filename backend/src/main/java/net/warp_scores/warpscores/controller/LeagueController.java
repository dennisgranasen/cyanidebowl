package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LeagueController {

    private final LeagueRepository leagueRepository;

    private final CompetitionService competitionService;

    @GetMapping("/league")
    public ResponseEntity<List<League>> getLeagues() {
        List<League> all = leagueRepository.findAll();
        all.forEach(this::countCompetitions);
        return ResponseEntity.ok(all);
    }

    @GetMapping("/league/{leagueUuid}")
    public ResponseEntity<League> getLeague(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        Optional<League> league = leagueRepository.findById(leagueUuid);
        league.ifPresent(this::countCompetitions);
        return league
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private void countCompetitions(League league) {
        Map<CompetitionStatus, Long> countsByStatus = competitionService.countForLeague(league.getUuid());
        league.setCountsByCompetitionStatus(countsByStatus);
    }
}
