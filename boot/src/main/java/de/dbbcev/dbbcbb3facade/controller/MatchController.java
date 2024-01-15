package de.dbbcev.dbbcbb3facade.controller;

import de.dbbcev.dbbcbb3facade.domain.MatchRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Match;
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
public class MatchController {

    private final MatchRepository matchRepository;

    @GetMapping("/matches/team/{teamUuid}")
    public ResponseEntity<List<Match>> getTeamMatches(@PathVariable(name = "teamUuid") UUID teamUuid) {
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

    @GetMapping("/matches/competition/{competitionId}")
    public ResponseEntity<List<Match>> getCompetitionMatches(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            List<Match> byCompetitionId = matchRepository
                    .findAll()
                    .stream()
                    .filter(m -> competitionId.equals(m.getCompetitionId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(byCompetitionId);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
