package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Coach;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.warp_scores.warpscores.CacheNames.ARENA_COACH_TEAMS;
import static net.warp_scores.warpscores.CacheNames.ARENA_INFOS;
import static net.warp_scores.warpscores.CacheNames.ARENA_RACES;
import static net.warp_scores.warpscores.CacheNames.ARENA_TEAMS;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArenaService {

    private static final int NEEDED_WINS_FOR_COMPLETION = 7;
    private static final int LOSSES_BEFORE_ELIMINATION = 2;

    public enum RunType {completed, active, failed}

    private final ContestRepository contestRepository;

    @Cacheable(ARENA_RACES)
    public List<Race> loadArenaRacesFor(UUID competitionUuid) {
        return contestRepository.getUsedRacesForCompetition(competitionUuid);
    }

    @Cacheable(ARENA_INFOS)
    public Optional<ArenaInfo> loadArenaInfoFor(UUID competitionUuid, Race race) {
        List<ArenaTeam> arenaTeams = loadArenaTeamsFor(competitionUuid, race, Optional.empty(), Optional.empty());
        return toArenaInfo(race, arenaTeams);
    }

    @Cacheable(ARENA_TEAMS)
    public List<ArenaTeam> loadArenaTeamsFor(UUID competitionUuid, Race race, RunType runType) {
        List<ArenaTeam> teamsForRace = loadArenaTeamsFor(competitionUuid, race, Optional.empty(),
                Optional.empty());
        List<ArenaTeam> filteredTeams = teamsForRace
                .stream()
                .filter(arenaTeam -> matchesRunType(arenaTeam, runType))
                .toList();
        filteredTeams
                .forEach(this::updateLogoAndNameFromContestsData);
        return filteredTeams;
    }

    @Cacheable(ARENA_COACH_TEAMS)
    public List<ArenaTeam> loadArenaTeamsFor(UUID competitionUuid, String coachId) {
        List<ArenaTeam> arenaTeams = contestRepository.queryArenaTeamsFor(
                competitionUuid,
                null, coachId, Pageable.unpaged());
        List<ArenaTeam> coachTeams = arenaTeams
                .stream()
                .filter(arenaTeam -> arenaTeam.getCoachUuid().equals(UUID.fromString(coachId)))
                .toList();
        coachTeams
                .forEach(this::updateLogoAndNameFromContestsData);
        return coachTeams;
    }

    private void updateLogoAndNameFromContestsData(ArenaTeam arenaTeam) {
        Optional<Team> team = arenaTeam
                .getContests()
                .stream()
                .sorted(Comparator.comparing(Contest::getMatchDate).reversed())
                .map(Contest::getOpponents)
                .flatMap(Collection::stream)
                .filter(t -> t.getId().equals(arenaTeam.getTeamUuid()))
                .findFirst();
        team.ifPresent(t -> {
            arenaTeam.setTeamLogo(t.getLogo());
            arenaTeam.setTeamName(t.getName());
        });
    }

    private boolean matchesRunType(ArenaTeam arenaTeam, RunType runType) {
        final AtomicInteger completedCount = new AtomicInteger(0);
        final AtomicInteger failedCount = new AtomicInteger(0);
        final AtomicInteger activeCount = new AtomicInteger(0);
        countRunsInto(arenaTeam.getTeamUuid(), arenaTeam.getContests(), activeCount, completedCount, failedCount);
        return switch (runType) {
            case completed -> completedCount.get() > 0;
            case active -> activeCount.get() > 0;
            case failed -> failedCount.get() > 0;
        };
    }

    private List<ArenaTeam> loadArenaTeamsFor(UUID competitionUuid,
            Race race, Optional<Integer> limit, Optional<Integer> offset) {
        StopWatch stopWatch = new StopWatch();
        try {
            String taskName = String.format("getArenaContestsBy[%s]", race);
            stopWatch.start(taskName);
            return getArenaTeamsFor(competitionUuid, race, limit, offset);
        } finally {
            stopWatch.stop();
            StopWatch.TaskInfo taskInfo = stopWatch.lastTaskInfo();
            log.info("StopWatch {}: {}ms", taskInfo.getTaskName(), taskInfo.getTimeMillis());
        }
    }

    Optional<ArenaInfo> toArenaInfo(final Race race, List<ArenaTeam> arenaTeams) {
        if (arenaTeams.isEmpty()) {
            return Optional.empty();
        }
        Set<UUID> coachIds = new HashSet<>();
        Set<UUID> teamIds = new HashSet<>();
        Set<UUID> contestUuids = new HashSet<>();
        Map<UUID, Integer> winsByTeamId = new HashMap<>();
        Map<UUID, Integer> lossesByTeamId = new HashMap<>();
        Map<UUID, Integer> failedRunsByTeamId = new HashMap<>();
        Map<UUID, Integer> completedRunsByTeamId = new HashMap<>();
        Map<UUID, Integer> activeRunsByTeamId = new HashMap<>();
        arenaTeams
                .stream()
                .filter(arenaTeam -> race.equals(arenaTeam.getRace()))
                .forEach(arenaTeam -> {
                    coachIds.add(arenaTeam.getCoachUuid());
                    contestUuids.addAll(
                            arenaTeam.getContests().stream()
                                    .filter(c -> c.getOpponents().stream().map(Team::getRace)
                                            .anyMatch(r -> r.equals(race)))
                                    .map(Contest::getContestUuid).collect(Collectors.toSet()));
                    UUID teamUuid = arenaTeam.getTeamUuid();
                    teamIds.add(teamUuid);
                    Integer wins = arenaTeam
                            .getResults()
                            .stream()
                            .filter(r -> r.getResult() == ArenaTeam.Result.ResultType.win)
                            .mapToInt(ArenaTeam.Result::getCount)
                            .sum();
                    winsByTeamId.putIfAbsent(teamUuid, wins);
                    Integer losses = arenaTeam
                            .getResults()
                            .stream()
                            .filter(r -> r.getResult() == ArenaTeam.Result.ResultType.loss)
                            .mapToInt(ArenaTeam.Result::getCount)
                            .sum();
                    lossesByTeamId.putIfAbsent(teamUuid, losses);

                    final AtomicInteger completedCount = new AtomicInteger(0);
                    final AtomicInteger failedCount = new AtomicInteger(0);
                    final AtomicInteger activeCount = new AtomicInteger(0);
                    countRunsInto(teamUuid, arenaTeam.getContests(), activeCount, completedCount, failedCount);
                    completedRunsByTeamId.putIfAbsent(teamUuid, completedCount.get());
                    failedRunsByTeamId.putIfAbsent(teamUuid, failedCount.get());
                    activeRunsByTeamId.putIfAbsent(teamUuid, activeCount.get());
                });
        ArenaInfo arenaInfo = new ArenaInfo()
                .withRace(race)
                .withCoaches(coachIds.size())
                .withTeams(teamIds.size())
                .withMatches(contestUuids.size())
                .withWins(winsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withLosses(lossesByTeamId.values().stream().mapToInt(l -> l).sum())
                .withActiveRuns(activeRunsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withCompletedRuns(completedRunsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withFailedRuns(failedRunsByTeamId.values().stream().mapToInt(l -> l).sum());
        return Optional.of(arenaInfo);
    }

    private void countRunsInto(UUID teamUuid,
            List<Contest> contests,
            AtomicInteger activeCount,
            AtomicInteger completedCount,
            AtomicInteger failedCount) {
        contests.sort(Comparator.comparing(Contest::getMatchDate));
        int wins = 0, losses = 0;
        for (Contest contest : contests) {
            UUID winnerUuid = getWinnerTeamUuidOrNull(contest);
            if (winnerUuid == null) {
                continue;
            }
            if (teamUuid.equals(winnerUuid)) {
                wins++;
            } else {
                losses++;
            }
            if (wins == NEEDED_WINS_FOR_COMPLETION) {
                completedCount.getAndIncrement();
                wins = 0;
                losses = 0;
            }
            if (losses == LOSSES_BEFORE_ELIMINATION) {
                failedCount.getAndIncrement();
                wins = 0;
                losses = 0;
            }
        }
        if (wins > 0 || losses > 0) {
            activeCount.getAndIncrement();
        }
    }

    @SuppressWarnings("unchecked")
    private UUID getWinnerTeamUuidOrNull(Contest contest) {
        return Optional.ofNullable((Map<String, Object>) contest.getWinner())
                .map(winner -> (Map<String, Object>) winner.get("team"))
                .map(team -> (String) team.get("id"))
                .map(UUID::fromString)
                .orElse(null);
    }

    private List<ArenaTeam> getArenaTeamsFor(UUID competitionUuid,
            Race race,
            Optional<Integer> limit,
            Optional<Integer> offset) {

        Pageable pageable = limit
                .map(l -> pageableFor(l, offset))
                .orElse(Pageable.unpaged());
        return contestRepository.queryArenaTeamsFor(
                competitionUuid,
                race, null, pageable);
    }

    private String raceToPattern(Optional<Race> race) {
        return String.format("/%s/", race.map(Race::getRaceName).orElse(".*"));
    }

    private String uuidToPattern(Optional<UUID> uuid) {
        return String.format("/%s/", uuid.map(UUID::toString).orElse(".*"));
    }

    private Coach toCoach(ArenaTeam arenaTeam) {
        Coach coach = new Coach();
        coach.setName(arenaTeam.getCoachName());
        coach.setId(arenaTeam.getCoachUuid());
        return coach;
    }

    private Pageable pageableFor(Integer limit, Optional<Integer> offset) {
        return PageRequest
                .of(offset
                                .map(o -> o / limit)
                                .orElse(0),
                        limit,
                        Sort.by("coachName").ascending()
                                .and(Sort.by("teamName").ascending()));
    }
}
