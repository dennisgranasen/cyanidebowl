package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.model.Match;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class MatchController {

    private final MatchRepository matchRepository;
    private final CompetitionService competitionService;

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
            Optional<Competition> competition = competitionService.loadCompetition(competitionId);
            List<Match> byCompetitionId = matchRepository
                    .findByCompetitionId(competitionId);
            List<Match> matches = initializeForCompetition(byCompetitionId, competition);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<Match> initializeForCompetition(List<Match> matches, Optional<Competition> competition) {
        Stream<Match> sorted = matches
                .stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(Match::getStarted)));
        AtomicInteger matchNumber = new AtomicInteger(1);
        sorted.forEach(m -> setRound(m, matchNumber.getAndIncrement(), competition));
        return sorted.collect(Collectors.toList());
    }

    private void setRound(Match match, Integer currentMatchNumber, Optional<Competition> competition) {
        competition
                .ifPresent(c -> setRound(match, currentMatchNumber, c));
    }

    public void setRound(Match match, Integer currentMatchNumber, Competition c) {
        match.setRound(determineRound(currentMatchNumber, c));
    }

    private Integer determineRound(Integer currentMatchNumber, Competition competition) {
        int matchesPerRound = competition.getTeamsMax() / 2;
        return currentMatchNumber / matchesPerRound + 1;
    }
}
