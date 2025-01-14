package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Coach;
import net.warp_scores.warpscores.model.Match;
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

    private final MatchRepository matchRepository;

    @Cacheable(ARENA_RACES)
    public List<Race> loadArenaRacesFor(UUID competitionUuid) {
        return matchRepository.getUsedRacesForCompetition(competitionUuid);
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
    public List<ArenaTeam> loadArenaTeamsFor(UUID competitionUuid, UUID coachId) {
        List<ArenaTeam> arenaTeams = matchRepository.queryArenaTeamsFor(
                competitionUuid,
                null, coachId, Pageable.unpaged());
        List<ArenaTeam> coachTeams = arenaTeams
                .stream()
                .filter(arenaTeam -> arenaTeam.getCoachUuid().equals(coachId))
                .toList();
        coachTeams
                .forEach(this::updateLogoAndNameFromContestsData);
        return coachTeams;
    }

    private void updateLogoAndNameFromContestsData(ArenaTeam arenaTeam) {
        Optional<Team> team = arenaTeam
                .getMatches()
                .stream()
                .sorted(Comparator.comparing(Match::getFinished).reversed())
                .map(Match::getTeams)
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
        countRunsInto(arenaTeam.getTeamUuid(), arenaTeam.getMatches(), activeCount, completedCount, failedCount);

        final AtomicInteger completedCountForLast9Games = new AtomicInteger(0);
        final AtomicInteger failedCountForLast9Games = new AtomicInteger(0);
        final AtomicInteger activeCountForLast9Games = new AtomicInteger(0);
        countRunsInto(arenaTeam.getTeamUuid(), arenaTeam.getMatches(), activeCountForLast9Games,
                completedCountForLast9Games, failedCountForLast9Games, true, Optional.of(9));

        if (completedCount.get() == 0 && completedCountForLast9Games.get() > 0) {
            completedCount.getAndIncrement();
        }

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
        Set<UUID> matchUuids = new HashSet<>();
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
                    matchUuids.addAll(
                            arenaTeam
                                    .getMatches()
                                    .stream()
                                    .filter(c -> c.getTeams().stream().map(Team::getRace)
                                            .anyMatch(r -> r.equals(race)))
                                    .map(Match::getMatchId)
                                    .collect(Collectors.toSet()));
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
                    countRunsInto(teamUuid, arenaTeam.getMatches(), activeCount, completedCount, failedCount);

                    final AtomicInteger completedCountForLast9Games = new AtomicInteger(0);
                    final AtomicInteger failedCountForLast9Games = new AtomicInteger(0);
                    final AtomicInteger activeCountForLast9Games = new AtomicInteger(0);
                    countRunsInto(teamUuid, arenaTeam.getMatches(), activeCountForLast9Games,
                            completedCountForLast9Games, failedCountForLast9Games, true, Optional.of(9));

                    if (completedCount.get() == 0 && completedCountForLast9Games.get() > 0) {
                        completedCount.getAndIncrement();
                    }
                    completedRunsByTeamId.putIfAbsent(teamUuid, completedCount.get());
                    failedRunsByTeamId.putIfAbsent(teamUuid, failedCount.get());
                    activeRunsByTeamId.putIfAbsent(teamUuid, activeCount.get());
                });
        ArenaInfo arenaInfo = new ArenaInfo()
                .withRace(race)
                .withCoaches(coachIds.size())
                .withTeams(teamIds.size())
                .withMatches(matchUuids.size())
                .withWins(winsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withLosses(lossesByTeamId.values().stream().mapToInt(l -> l).sum())
                .withActiveRuns(activeRunsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withCompletedRuns(completedRunsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withFailedRuns(failedRunsByTeamId.values().stream().mapToInt(l -> l).sum());
        return Optional.of(arenaInfo);
    }

    private void countRunsInto(UUID teamUuid,
            List<Match> matches,
            AtomicInteger activeCount,
            AtomicInteger completedCount,
            AtomicInteger failedCount) {
        countRunsInto(teamUuid, matches, activeCount, completedCount, failedCount, false, Optional.empty());
    }

    private void countRunsInto(UUID teamUuid,
            List<Match> matches,
            AtomicInteger activeCount,
            AtomicInteger completedCount,
            AtomicInteger failedCount,
            boolean goBackwards,
            Optional<Integer> limit) {
        Comparator<Match> comparing = Comparator.comparing(Match::getFinished);
        if (goBackwards) {
            comparing = comparing.reversed();
        }
        AtomicInteger wins = new AtomicInteger(0);
        AtomicInteger losses = new AtomicInteger(0);
        matches
                .stream()
                .sorted(comparing)
                .limit(limit.orElse(matches.size()))
                .forEach(match -> countContestInto(teamUuid, match, wins, losses, completedCount,
                        failedCount));
        if (wins.get() > 0 || losses.get() > 0) {
            activeCount.getAndIncrement();
        }

    }

    private void countContestInto(UUID teamUuid, Match match,
            AtomicInteger currWins,
            AtomicInteger currLosses,
            AtomicInteger completedCount,
            AtomicInteger failedCount) {
        UUID winnerUuid = getWinnerTeamUuidOrNull(match);
        if (winnerUuid == null) {
            return;
        }
        if (teamUuid.equals(winnerUuid)) {
            currWins.getAndIncrement();
        } else {
            currLosses.getAndIncrement();
        }
        if (currWins.get() == NEEDED_WINS_FOR_COMPLETION) {
            completedCount.getAndIncrement();
            currWins.set(0);
            currLosses.set(0);
        }
        if (currLosses.get() == LOSSES_BEFORE_ELIMINATION) {
            failedCount.getAndIncrement();
            currWins.set(0);
            currLosses.set(0);
        }
    }

    private UUID getWinnerTeamUuidOrNull(Match match) {
        return match
                .getTeams()
                .stream()
                .max(Comparator.comparing(Team::getScore))
                .map(Team::getId)
                .orElse(null);
    }

    private List<ArenaTeam> getArenaTeamsFor(UUID competitionUuid,
            Race race,
            Optional<Integer> limit,
            Optional<Integer> offset) {

        Pageable pageable = limit
                .map(l -> pageableFor(l, offset))
                .orElse(Pageable.unpaged());

        return matchRepository.queryArenaTeamsFor(
                competitionUuid,
                race, null, pageable);
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
