package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.MatchService;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MatchController {
    public static final int MAX_LIMIT_FOR_LATEST_MATCHES = 24;
    public static final int DEFAULT_LIMIT_FOR_LATEST_MATCHES = 6;

    private final CompetitionService competitionService;
    private final MatchService matchService;

    @GetMapping("/matches/team/{teamUuid}")
    public ResponseEntity<List<Match>> getTeamMatches(@PathVariable(name = "teamUuid") UUID teamUuid) {
        try {
            List<Match> byTeamId = matchService.findByTeamId(teamUuid);
            return ResponseEntity.ok(byTeamId);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches for team {}", teamUuid, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/league/{leagueUuid}/latest")
    public ResponseEntity<List<Match>> getLatestLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        return getLatestLeagueMatches(leagueUuid, null);
    }

    @GetMapping("/matches/league/{leagueUuid}/latest/{limit}")
    public ResponseEntity<List<Match>> getLatestLeagueMatches(@PathVariable(name = "leagueUuid") UUID leagueUuid,
            @PathVariable(name = "limit") Integer limit) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_MATCHES);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_MATCHES);
        try {
            List<Match> matches = matchService.getLatestLeagueMatches(leagueUuid, limit);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/competition/{competitionId}")
    public ResponseEntity<List<Match>> getCompetitionMatches(
            @PathVariable(name = "competitionId") UUID competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Optional<Competition> competition =
                competitionService.loadCompetition(competitionId, Optional.of(opus));
            List<Match> byCompetitionId = matchService.findByCompetitionId(competitionId);
            List<Match> matches = initializeForCompetition(byCompetitionId, competition);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches for competition {}", competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/competition/{competitionId}/latest")
    public ResponseEntity<List<Match>> getLatestCompetitionMatches(@PathVariable(name = "competitionId") UUID competitionId) {
        return getLatestCompetitionMatches(competitionId, null);
    }

    @GetMapping("/matches/competition/{competitionId}/latest/{limit}")
    public ResponseEntity<List<Match>> getLatestCompetitionMatches(@PathVariable(name = "competitionId") UUID competitionId,
            @PathVariable(name = "limit") Integer limit) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_MATCHES);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_MATCHES);
        try {
            List<Match> matches = matchService.getLatestCompetitionMatches(competitionId, limit);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<Match> initializeForCompetition(List<Match> matches, Optional<Competition> competition) {
        List<Match> sorted = matches
                .stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(Match::getStarted)))
                .toList();
        AtomicInteger matchNumber = new AtomicInteger(1);
        sorted.forEach(m -> setRound(m, matchNumber.getAndIncrement(), competition));
        return sorted;
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
