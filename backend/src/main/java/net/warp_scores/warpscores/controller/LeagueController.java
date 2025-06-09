package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.service.LeagueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    @GetMapping("/leagues/{id}")
    public ResponseEntity<League> getLeague(
        @PathVariable(name = "id") String leagueId,
        @RequestParam(name = "opus", required = false) Integer opus) {
        log.info("Fetching league with ID: {} and opus: {}", leagueId, opus);
        Optional<League> league;
        try {
            UUID uuid = UUID.fromString(leagueId);
            league = leagueService.loadById(uuid, Optional.ofNullable(opus));
        } catch (IllegalArgumentException e) {
            // Not a UUID, try as integer
            try {
                Integer oldId = Integer.valueOf(leagueId);
                league = leagueService.loadByOldId(oldId, Optional.ofNullable(opus));
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().build();
            }
        }
        return league
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
