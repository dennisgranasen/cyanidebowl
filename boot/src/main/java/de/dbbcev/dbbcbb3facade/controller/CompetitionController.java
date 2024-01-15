package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionStatus;
import de.dbbcev.dbbcbb3facade.domain.CompetitionRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionRepository competitionRepository;

    @GetMapping("/competitions/league/{leagueId}/{status}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeagueAndStatus(@PathVariable(name = "leagueId") UUID leagueId,
            @PathVariable(name = "status")
            CompetitionStatus status) {
        try {
            List<Competition> competitions = competitionRepository.findByLeagueIdAndStatus(leagueId, status);
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeague(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Competition> competitions = competitionRepository.findByLeagueId(leagueId);
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionId}")
    private ResponseEntity<Competition> getCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            Optional<Competition> competition = competitionRepository.findById(competitionId);
            return competition
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
