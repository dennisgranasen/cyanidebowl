package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamDomainService teamDomainService;

    private final MatchDomainService matchDomainService;

    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<Team> getTeam(
            @PathVariable(name = "teamId") String teamId,
            @RequestParam(name = "opus", required = false) Integer opus ) {
        try {
            Identity teamIdentity = new SimpleIdentity(teamId, ofNullable(opus).orElse(3));
            Optional<Team> team = teamDomainService.findTeam(teamIdentity, Optional.empty());
            return team
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Unable to get team for id {}.", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/teams/{teamId}/matches")
    public ResponseEntity<List<Match>> getMatches(
            @PathVariable(name = "teamId") String teamId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        try {

            Identity teamIdentity = 
                new SimpleIdentity(teamId, ofNullable(opus).orElse(defaultOpus));
            List<Match> matchesForTeam = 
                matchDomainService.findMatchesForTeam(teamIdentity);
            matchesForTeam.forEach(match -> officialLeagueAndCompetitions.adjustCompetitionName(match.getLeagueId(),
                    match.getCompetitionName(), match::setCompetitionName));
            return ResponseEntity.ok(matchesForTeam);
        } catch (Exception ex) {
            log.error("Unable to get matches for team uuid {}.", teamId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
