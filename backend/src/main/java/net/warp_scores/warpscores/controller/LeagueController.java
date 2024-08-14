package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.LeagueService;
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

    private final LeagueService leagueService;

    @GetMapping("/leagues")
    public ResponseEntity<List<League>> getLeagues() {
        List<League> all = leagueService.loadAll();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/leagues/{leagueUuid}")
    public ResponseEntity<League> getLeague(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        Optional<League> league = leagueService.loadById(leagueUuid);
        return league
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
