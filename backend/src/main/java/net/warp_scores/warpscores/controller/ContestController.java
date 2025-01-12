package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.ContestDomainService;
import net.warp_scores.warpscores.model.Coach;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ArenaTeam;
import net.warp_scores.warpscores.service.ContestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_LEAGUE_ADMIN;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ContestController {

    public static final int MAX_LIMIT_FOR_LATEST_CONTESTS = 12;
    public static final int DEFAULT_LIMIT_FOR_LATEST_CONTESTS = 6;
    public static final int DEFAULT_LIMIT_FOR_LIVE_CONTESTS = 15;
    public static final int DEFAULT_LIMIT_FOR_CONTESTS = 100;
    private final ContestService contestService;
    private final ContestDomainService contestDomainService;

    @GetMapping("/contests/competition/{competitionUuid}")
    public ResponseEntity<List<Contest>> getCompetitionContests(@PathVariable(name = "competitionUuid") UUID competitionUuid) {
        return getCompetitionContests(competitionUuid, null);
    }

    @GetMapping("/contests/competition/{competitionUuid}/{limit}")
    public ResponseEntity<List<Contest>> getCompetitionContests(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "limit") Integer limit) {
        try {
            List<Contest> contests = contestService.getCompetitionContests(competitionUuid, Optional.ofNullable(limit));
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/contests/competition/{competitionUuid}")
    @PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN) // ✨
    public ResponseEntity<Void> addContest(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @RequestBody Contest contest) {
        try {
            contestDomainService.addContest(contest);
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            log.error("Unable to add contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/league/{leagueUuid}/latest")
    public ResponseEntity<List<Contest>> getLatestLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        return getLatestLeagueContests(leagueUuid, null);
    }

    @GetMapping("/contests/league/{leagueUuid}/latest/{limit}")
    public ResponseEntity<List<Contest>> getLatestLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid,
            @PathVariable(name = "limit") Integer limit) {
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LATEST_CONTESTS);
        limit = Math.min(limit, MAX_LIMIT_FOR_LATEST_CONTESTS);
        try {
            List<Contest> contests = contestService.getLatestLeagueContests(leagueUuid, limit);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/league/{leagueUuid}/live")
    public ResponseEntity<List<Contest>> getLiveLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid) {
        return getLiveLeagueContests(leagueUuid, null);
    }

    @GetMapping("/contests/league/{leagueUuid}/live/{limit}")
    public ResponseEntity<List<Contest>> getLiveLeagueContests(@PathVariable(name = "leagueUuid") UUID leagueUuid,
            @PathVariable(name = "limit") Integer limit) {
        try {
            limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT_FOR_LIVE_CONTESTS);
            List<Contest> contests = contestService.getLiveLeagueContests(leagueUuid, limit);
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/contests/competition/{competitionUuid}/arena/{race}")
    public ResponseEntity<Map<Coach, List<ArenaTeam>>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") Race race) {
        return getArenaTeamsFor(competitionUuid, race, null);
    }

    @GetMapping("/contests/competition/{competitionUuid}/arena/{race}/{limit}")
    public ResponseEntity<Map<Coach, List<ArenaTeam>>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") Race race, @PathVariable(name = "limit") Integer limit) {
        return getArenaTeamsFor(competitionUuid, race, Optional.ofNullable(limit), 0);
    }

    @GetMapping("/contests/competition/{competitionUuid}/arena/{race}/{limit}/{offset}")
    public ResponseEntity<Map<Coach, List<ArenaTeam>>> getArenaTeamsFor(@PathVariable(name = "competitionUuid") UUID competitionUuid,
            @PathVariable(name = "race") Race race,
            @PathVariable(name = "limit") Optional<Integer> limit,
            @PathVariable(name = "offset") Integer offset) {
        List<ArenaTeam> teamsForRace = loadArenaTeamsFor(competitionUuid, race, limit, Optional.ofNullable(offset));
        ConcurrentMap<Coach, List<ArenaTeam>> teamsByCoach = teamsForRace.stream()
                .collect(Collectors.groupingByConcurrent(this::toCoach, Collectors.toList()));
        return ResponseEntity.ok(teamsByCoach);
    }

    @GetMapping("/contests/competition/{competitionUuid}/arena")
    public ResponseEntity<Map<Race, Long>> getArenaContests(@PathVariable(name = "competitionUuid") UUID competitionUuid) {
        Map<Race, Long> contestsByRace = new HashMap<>();
        for (Race race : Race.values()) {
            int teamCount = loadArenaTeamsFor(competitionUuid, race, Optional.empty(), Optional.empty()).size();
            if (teamCount > 0) {
                contestsByRace.put(race, (long) teamCount);
            }
        }
        return ResponseEntity.ok(contestsByRace);
    }

    private List<ArenaTeam> loadArenaTeamsFor(UUID competitionUuid,
            Race race, Optional<Integer> limit, Optional<Integer> offset) {
        StopWatch stopWatch = new StopWatch();
        try {
            String taskName = String.format("getArenaContestsBy[%s]", race);
            stopWatch.start(taskName);
            return contestService.getTeamsWithGamesMoreThan(competitionUuid, race, 0, limit, offset);
        } finally {
            stopWatch.stop();
            StopWatch.TaskInfo taskInfo = stopWatch.lastTaskInfo();
            log.info("StopWatch {}: {}ms", taskInfo.getTaskName(), taskInfo.getTimeMillis());
        }
    }

    private Coach toCoach(ArenaTeam arenaTeam) {
        Coach coach = new Coach();
        coach.setName(arenaTeam.getCoachName());
        coach.setId(arenaTeam.getCoachUuid());
        return coach;
    }
}
