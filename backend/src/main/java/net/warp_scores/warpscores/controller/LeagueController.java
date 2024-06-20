package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.League;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LeagueController {

    private final LeagueRepository leagueRepository;

    @GetMapping("/league")
    public ResponseEntity<List<League>> getLeagues() {
        List<League> all = leagueRepository.findAll();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/league/{leagueUuid}")
    public ResponseEntity<League> getLeague(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        Optional<League> league = leagueRepository.findById(leagueUuid);
        return league
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
