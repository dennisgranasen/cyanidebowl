package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.service.CompetitionService;
import net.warp_scores.warpscores.service.MatchService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MatchController {
    public static final int MAX_LIMIT_FOR_LATEST_MATCHES = 24;
    public static final int DEFAULT_LIMIT_FOR_LATEST_MATCHES = 6;

    private final CompetitionService competitionService;
    private final MatchService matchService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @GetMapping("/matches/team/{teamId}")
    public ResponseEntity<List<Match>> getTeamMatches(
            @PathVariable(name = "teamId") String teamId, 
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            List<Match> byTeamId = 
            matchService.findByTeamId(new SimpleIdentity(
                teamId, Optional.ofNullable(opus).orElse(defaultOpus)));
            return ResponseEntity.ok(byTeamId);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches for team {}", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/league/{leagueId}/latest")
    public ResponseEntity<List<Match>> getLatestLeagueContests(
        @PathVariable(name = "leagueId") String leagueId,
        @RequestParam(name = "opus", required = false) Integer opus) {
        return getLatestLeagueMatches(leagueId, null, opus);
    }

    @GetMapping("/matches/league/{leagueId}/latest/{limit}")
    public ResponseEntity<List<Match>> getLatestLeagueMatches(
        @PathVariable(name = "leagueId") String leagueId,
            @PathVariable(name = "limit") Integer limit,
            @RequestParam(name = "opus", required = false) Integer opus) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_MATCHES);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_MATCHES);
        try {
            Identity leagueIdentity = new SimpleIdentity(
                leagueId, Optional.ofNullable(opus).orElse(defaultOpus));
            List<Match> matches = matchService.getLatestLeagueMatches(leagueIdentity, limit);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/competition/{competitionId}")
    public ResponseEntity<List<Match>> getCompetitionMatches(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {
            Identity competitionIdentity = new SimpleIdentity(
                competitionId, Optional.ofNullable(opus).orElse(defaultOpus));
            Optional<Competition> competition =
                competitionService.loadCompetition(competitionIdentity);
            List<Match> byCompetitionId = matchService.findByCompetitionId(competitionIdentity);
            List<Match> matches = initializeForCompetition(byCompetitionId, competition);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches for competition {}", 
                competitionId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matches/competition/{competitionId}/latest")
    public ResponseEntity<List<Match>> getLatestCompetitionMatches(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        return getLatestCompetitionMatches(competitionId, null, opus);
    }

    @GetMapping("/matches/competition/{competitionId}/latest/{limit}")
    public ResponseEntity<List<Match>> getLatestCompetitionMatches(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "limit") Integer limit,
            @RequestParam(name = "opus", required = false) Integer opus) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_MATCHES);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_MATCHES);
        try {
            Identity competitionIdentity = 
                new SimpleIdentity(competitionId, Optional.ofNullable(opus).orElse(defaultOpus));

            List<Match> matches = matchService.getLatestCompetitionMatches(
                competitionIdentity, limit);
            return ResponseEntity.ok(matches);
        } catch (Exception ex) {
            log.error("Unable to retrieve matches", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<Match> initializeForCompetition(
            List<Match> matches, Optional<Competition> competition) {
        List<Match> sorted = matches
                .stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(Match::getStarted)))
                .toList();
        AtomicInteger matchNumber = new AtomicInteger(1);
        sorted.forEach(m -> setRound(m, matchNumber.getAndIncrement(), competition));
        return sorted;
    }

    private void setRound(Match match, Integer currentMatchNumber,
            Optional<Competition> competition) {
        competition
                .ifPresent(c -> setRound(match, currentMatchNumber, c));
    }

    public void setRound(Match match, Integer currentMatchNumber, Competition c) {
        match.setRound(determineRound(currentMatchNumber, c).toString());
    }

    private Integer determineRound(Integer currentMatchNumber, Competition competition) {
        int matchesPerRound = competition.getTeamsMax() / 2;
        return currentMatchNumber / matchesPerRound + 1;
    }
}
