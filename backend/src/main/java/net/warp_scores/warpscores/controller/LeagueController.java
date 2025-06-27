package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.requests.IdentifiablesRequest;
import net.warp_scores.warpscores.service.LeagueService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.ipc.http.HttpSender.Response;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@Slf4j
public class LeagueController {

    private final LeagueService leagueService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

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

            league = leagueService.loadById(new SimpleIdentity(leagueId, 
                Optional.ofNullable(opus).orElse(defaultOpus)));
            league.ifPresentOrElse(l -> log.info("Fetched league: {}", l),
                                    () -> log.warn("League not found: {}", leagueId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return league
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/leagues/competitionCountByStatus/")
    public ResponseEntity<Map<Identity,Map<CompetitionStatus, Integer>>> getCompetitionCountByStatus(
        @PathVariable(name = "id") String leagueIds) {
        String[] ids = leagueIds.split(",");
        Collection<Identity> ids = leagues.getIdentifiables();
        if (ids == null || ids.isEmpty()) {
            log.warn("No leagues provided for competition count");
            return ResponseEntity.badRequest().build();
        }
        try {
            return  
                ResponseEntity.ok(leagueService.getCompetitionCountByStatus(ids));
        } catch (Exception e) {
            log.error("Error fetching competition count by status", e);
            return ResponseEntity.status(500).build();
        }   
    }
    

    /*
      fetchCompetitionCountByStatus: async (leagues) => 
    axios.post(`/leagues/competitionCountByStatus`, 
     { leagues: leagues.map((l) => l.id.key) }) 
      .then(returnData).catch(handleError)
    */
}
