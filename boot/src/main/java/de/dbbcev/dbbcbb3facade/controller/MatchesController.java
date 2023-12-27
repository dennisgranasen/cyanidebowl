package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.domain.MatchRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Match;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MatchesController {

    private final CyanideApiService cyanideApiService;

    private final MatchRepository matchRepository;

    @GetMapping("/matches/{teamUuid}")
    public ResponseEntity<List<Match>> getMatches(@PathVariable UUID teamUuid) {
        try {
            List<Match> byTeamId = matchRepository
                    .findAll()
                    .stream()
                    .filter(t -> t.getTeams().contains(teamUuid))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(byTeamId);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
