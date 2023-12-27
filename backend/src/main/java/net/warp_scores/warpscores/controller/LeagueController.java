package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.domain.model.League;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
