package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ArenaService;
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
public class ArenaController {

    private final ArenaService arenaService;

    @GetMapping("/arena/{competitionUuid}/info")
    public ResponseEntity<List<Race>> getArenaRaces(@PathVariable(name = "competitionUuid") UUID competitionUuid) {
        List<Race> arenaRaces = arenaService.loadArenaRacesFor(competitionUuid);
        return ResponseEntity.ok(arenaRaces);
    }

    @GetMapping("/arena/{competitionUuid}/info/{race}")
    public ResponseEntity<ArenaInfo> getArenaInfoFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") String raceValue) {
        Race race = Race.valueOf(raceValue);
        Optional<ArenaInfo> arenaInfo = arenaService.loadArenaInfoFor(competitionUuid, race);
        return arenaInfo
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/arena/{competitionUuid}/race/{race}/{runType}")
    public ResponseEntity<List<ArenaTeam>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") Race race, @PathVariable(name = "runType") ArenaService.RunType runType) {
        return getArenaTeamsFor(competitionUuid, race, runType, null);
    }

    @GetMapping("/arena/{competitionUuid}/race/{race}/{runType}/{limit}")
    public ResponseEntity<List<ArenaTeam>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaService.RunType runType,
            @PathVariable(name = "limit") Integer limit) {
        return getArenaTeamsFor(competitionUuid, race, runType, Optional.ofNullable(limit), 0);
    }

    @GetMapping("/arena/{competitionUuid}/race/{race}/{runType}/{limit}/{offset}")
    public ResponseEntity<List<ArenaTeam>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "runType") ArenaService.RunType runType,
            @PathVariable(name = "limit") Optional<Integer> limit,
            @PathVariable(name = "offset") Integer offset) {
        List<ArenaTeam> arenaTeams = arenaService.loadArenaTeamsFor(competitionUuid, race, runType);
        return ResponseEntity.ok(arenaTeams);
    }

    @GetMapping("/arena/{competitionUuid}/coach/{coachId}")
    public ResponseEntity<List<ArenaTeam>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "coachId") String coachId) {
        List<ArenaTeam> arenaTeams = arenaService.loadArenaTeamsFor(competitionUuid, coachId);
        return ResponseEntity.ok(arenaTeams);
    }
}

