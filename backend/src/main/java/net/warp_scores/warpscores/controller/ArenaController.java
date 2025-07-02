package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.ArenaCoach;
import net.warp_scores.warpscores.model.ArenaCoachWithArenaTeams;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ArenaService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ArenaController {

    private final ArenaService arenaService;
    
    @Value("${cyanide.defaults.topCoaches:6}")
    private int defaultTopCoaches;

    private static final int DEFAULT_LIMIT_FOR_ARENA_TEAMS = 100;

    @GetMapping("/arena/{competitionId}/info")
    public ResponseEntity<List<Race>> getArenaRaces(
            @PathVariable(name = "competitionId") String competitionId) {
        Identity competitionIdentity = IdentityUtil.fromId(competitionId);
        List<Race> arenaRaces = arenaService.loadArenaRacesFor(competitionIdentity);
        return ResponseEntity.ok(arenaRaces);
    }

    @GetMapping("/arena/{competitionId}/info/{race}")
    public ResponseEntity<ArenaInfo> getArenaInfoFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "race") String raceValue) {
        Race race = Race.valueOf(raceValue);
        Identity competitionIdentity = IdentityUtil.fromId(competitionId);
        Optional<ArenaInfo> arenaInfo = arenaService.loadArenaInfoFor(competitionIdentity, race);
        return arenaInfo
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/arena/{competitionId}/race/{race}/{runType}")
    public ResponseEntity<Map<ArenaTeam.RunType, List<ArenaTeam>>> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaTeam.RunType runType) {
        return getArenaTeamsFor(competitionId, race, runType, DEFAULT_LIMIT_FOR_ARENA_TEAMS);
    }

    @GetMapping("/arena/{competitionId}/race/{race}/{runType}/{limit}")
    public ResponseEntity<Map<ArenaTeam.RunType, List<ArenaTeam>>> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaTeam.RunType runType,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return getArenaTeamsFor(competitionId, race, runType, limit, 0);
    }

    @GetMapping("/arena/{competitionId}/race/{race}/{runType}/{limit}/{offset}")
    public ResponseEntity<Map<ArenaTeam.RunType, List<ArenaTeam>>> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaTeam.RunType runType,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset) {
        
        Identity competitionIdentity = IdentityUtil.fromId(competitionId);

        Map<ArenaTeam.RunType, List<ArenaTeam>> arenaTeams = 
            arenaService.loadArenaTeamsFor(competitionIdentity, race,
                runType, Optional.ofNullable(limit), Optional.ofNullable(offset));
        return ResponseEntity.ok(arenaTeams);
    }

    @GetMapping("/arena/{competitionId}/topCoaches")
    public ResponseEntity<List<ArenaCoach>> getArenaTopCoaches(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "limit", required = false) Integer topLimit) {
        
        Identity competitionIdentity = IdentityUtil.fromId(competitionId);
        List<ArenaCoach> topCoaches = 
            arenaService.loadArenaTopCoachesFor(competitionIdentity, Optional.ofNullable(topLimit));
        return ResponseEntity.ok(topCoaches);
    }

    @GetMapping("/arena/{competitionId}/coach/{coachId}")
    public ResponseEntity<ArenaCoachWithArenaTeams> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "coachId") String coachId) {
    
        Identity competitionIdentity = IdentityUtil.fromId(competitionId);
        Identity coachIdentity = IdentityUtil.fromId(competitionId);

        ArenaCoachWithArenaTeams arenaCoachWithArenaTeams = 
            arenaService.loadArenaCoachWithArenaTeams(competitionIdentity, coachIdentity);
        return ResponseEntity.ok(arenaCoachWithArenaTeams);
    }
}

