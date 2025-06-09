package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.model.ArenaCoach;
import net.warp_scores.warpscores.model.ArenaCoachWithArenaTeams;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ArenaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ArenaController {

    private final ArenaService arenaService;

    @GetMapping("/arena/{competitionId}/info")
    public ResponseEntity<List<Race>> getArenaRaces(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        List<Race> arenaRaces = arenaService.loadArenaRacesFor(
            competitionId, 
            Optional.ofNullable(opus));
        return ResponseEntity.ok(arenaRaces);
    }

    @GetMapping("/arena/{competitionId}/info/{race}")
    public ResponseEntity<ArenaInfo> getArenaInfoFor(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus,
            @PathVariable(name = "race") String raceValue) {
        Race race = Race.valueOf(raceValue);
        Optional<ArenaInfo> arenaInfo = arenaService.loadArenaInfoFor(
            competitionId, 
            race,
            Optional.ofNullable(opus));
        return arenaInfo
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/arena/{competitionId}/race/{race}/{runType}")
    public ResponseEntity<Map<ArenaTeam.RunType, List<ArenaTeam>>> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaTeam.RunType runType,
            @RequestParam(name = "opus", required = false) Integer opus) {
        return getArenaTeamsFor(competitionId, race, runType, opus);
    }

    @GetMapping("/arena/{competitionId}/race/{race}/{runType}/{limit}")
    public ResponseEntity<Map<ArenaTeam.RunType, List<ArenaTeam>>> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaTeam.RunType runType,
            @PathVariable(name = "limit") Integer limit,
            @RequestParam(name = "opus", required = false) Integer opus) {
        return getArenaTeamsFor(competitionId, race, runType, limit, opus);
    }

    @GetMapping("/arena/{competitionId}/race/{race}/{runType}/{limit}/{offset}")
    public ResponseEntity<Map<ArenaTeam.RunType, List<ArenaTeam>>> getArenaTeamsFor(
            @PathVariable(name = "competitionUuid") String competitionId,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaTeam.RunType runType,
            @PathVariable(name = "limit") Integer limit,
            @PathVariable(name = "offset") Integer offset,
            @RequestParam(name = "opus", required = false) Integer opus) {
        Map<ArenaTeam.RunType, List<ArenaTeam>> arenaTeams = arenaService.loadArenaTeamsFor(competitionId, race,
                runType, Optional.ofNullable(limit), Optional.ofNullable(offset), opus);
        return ResponseEntity.ok(arenaTeams);
    }

    @GetMapping("/arena/{competitionId}/topCoaches")
    public ResponseEntity<List<ArenaCoach>> getArenaTopCoaches(
            @PathVariable(name = "competitionId") String competitionId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        List<ArenaCoach> topCoaches = arenaService.loadArenaTopCoachesFor(competitionId, opus);
        return ResponseEntity.ok(topCoaches);
    }

    @GetMapping("/arena/{competitionId}/coach/{coachId}")
    public ResponseEntity<ArenaCoachWithArenaTeams> getArenaTeamsFor(
            @PathVariable(name = "competitionId") String competitionId,
            @PathVariable(name = "coachId") String coachId,
            @RequestParam(name = "opus", required = false) Integer opus) {
        ArenaCoachWithArenaTeams arenaCoachWithArenaTeams = arenaService.loadArenaCoachWithArenaTeams(
            competitionId, coachId, opus);
        return ResponseEntity.ok(arenaCoachWithArenaTeams);
    }
}

