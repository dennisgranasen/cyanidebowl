package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.ArenaCoach;
import net.warp_scores.warpscores.model.ArenaCoachWithArenaTeams;
import net.warp_scores.warpscores.model.ArenaInfo;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.ArenaTeam.Result.ResultType;
import net.warp_scores.warpscores.model.Coach;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.WinRate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.warp_scores.warpscores.CacheNames.ARENA_COACHES;
import static net.warp_scores.warpscores.CacheNames.ARENA_COACH_TEAMS;
import static net.warp_scores.warpscores.CacheNames.ARENA_INFOS;
import static net.warp_scores.warpscores.CacheNames.ARENA_RACES;
import static net.warp_scores.warpscores.CacheNames.ARENA_TEAMS;
import static net.warp_scores.warpscores.model.ArenaTeam.RunType.active;
import static net.warp_scores.warpscores.model.ArenaTeam.RunType.completed;
import static net.warp_scores.warpscores.model.ArenaTeam.RunType.failed;
import static org.springframework.data.domain.Pageable.unpaged;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArenaService {

    private static final int NEEDED_WINS_FOR_COMPLETION = 7;
    private static final int LOSSES_BEFORE_ELIMINATION = 2;

    private final MatchRepository matchRepository;

    @Cacheable(ARENA_RACES)
    @DurationLogging
    public List<Race> loadArenaRacesFor(UUID competitionUuid) {
        return matchRepository.getUsedRacesForCompetition(competitionUuid);
    }

    @Cacheable(ARENA_INFOS)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public Optional<ArenaInfo> loadArenaInfoFor(UUID competitionUuid, Race race) {
        List<ArenaTeam> arenaTeams = loadArenaTeamsFor(competitionUuid, race, empty(), empty());
        return toArenaInfo(race, arenaTeams);
    }

    @Cacheable(ARENA_TEAMS)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public Map<ArenaTeam.RunType, List<ArenaTeam>> loadArenaTeamsFor(UUID competitionUuid,
            Race race,
            ArenaTeam.RunType runType, Optional<Integer> limit, Optional<Integer> offset) {
        List<ArenaTeam> teamsForRace = queryArenaTeamsFor(competitionUuid, race, unpaged());
        List<ArenaTeam> filteredTeams = teamsForRace
                .stream()
                .filter(arenaTeam -> matchesRunType(arenaTeam, runType))
                .filter(Objects::nonNull)
                .sorted(nullsLast(comparing(this::latestFinishedGame).reversed()))
                .toList();
        filteredTeams
                .forEach(this::updateLogoAndNameFromContestsData);
        List<ArenaTeam> limitedTeams = limit.map(l -> filteredTeams
                        .stream()
                        .skip(offset.orElse(0))
                        .limit(l)
                        .toList())
                .orElse(filteredTeams);
        log.info("Limited arenaTeams: {}", limitedTeams);
        return Map.of(runType, limitedTeams);
    }

    private Date latestFinishedGame(ArenaTeam arenaTeam) {
        return arenaTeam
                .getMatches()
                .stream()
                .map(Match::getFinished)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Cacheable(ARENA_COACH_TEAMS)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public Map<ArenaTeam.RunType, List<ArenaTeam>> loadArenaTeamsFor(UUID competitionUuid, UUID coachId) {
        return loadArenaTeamsForInternal(competitionUuid, coachId);
    }

    private Map<ArenaTeam.RunType, List<ArenaTeam>> loadArenaTeamsForInternal(UUID competitionUuid, UUID coachId) {
        List<ArenaTeam> arenaTeams = queryArenaTeamsFor(competitionUuid, coachId, unpaged());
        List<ArenaTeam> coachTeams = arenaTeams
                .stream()
                .filter(arenaTeam -> arenaTeam.getCoachUuid().equals(coachId))
                .sorted(comparing(this::latestFinishedGame).reversed())
                .toList();
        return toArenaTeamsByRunType(coachTeams);
    }

    private Map<ArenaTeam.RunType, List<ArenaTeam>> toArenaTeamsByRunType(List<ArenaTeam> coachTeams) {
        coachTeams
                .forEach(this::updateLogoAndNameFromContestsData);
        return coachTeams
                .stream()
                .collect(groupingBy(this::getRunTypeFor, toList()));
    }

    @Cacheable(ARENA_COACHES)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public List<ArenaCoach> loadArenaTopCoachesFor(UUID competitionUuid, int topLimit) {
        List<ArenaTeam> arenaTeams = new ArrayList<>();
        List<Race> races = loadArenaRacesFor(competitionUuid);
        for (Race race : races) {
            List<ArenaTeam> currQueryArenaTeams = queryArenaTeamsFor(competitionUuid, Optional.of(race), empty(), of(7),
                    unpaged());
            arenaTeams.addAll(currQueryArenaTeams);
            log.info("Fetched race {} with {} arena teams.", race, currQueryArenaTeams.size());
        }
        Map<Coach, List<ArenaTeam>> arenaTeamsByCoach = arenaTeams
                .stream()
                .collect(groupingBy(this::toCoach, toList()));

        List<ArenaCoach> arenaCoaches = new ArrayList<>();
        arenaTeamsByCoach.forEach(
                (coach, teams) -> arenaCoaches.add(toArenaCoach(coach, toArenaTeamsByRunType(teams))));
        return arenaCoaches
                .stream()
                .sorted(comparing(ArenaCoach::getCompletedRacesCount)
                        .thenComparing(ArenaCoach::getCompletedTeamsCount)
                        .thenComparing(ArenaCoach::getActiveNotCompletedRacesCount)
                        .reversed()
                        .thenComparing(ac -> ofNullable(ac.getLastCompletion()).orElse(new Date(0))))
                .limit(topLimit)
                .toList();
    }

    private ArenaCoach toArenaCoach(Coach coach, Map<ArenaTeam.RunType, List<ArenaTeam>> arenaTeamsByRunType) {
        ArenaCoach arenaCoach = new ArenaCoach();
        arenaCoach.setCoachName(coach.getName());
        arenaCoach.setCoachUuid(coach.getId());

        Map<ArenaTeam.RunType, Set<UUID>> teamUuidsByRunType = new HashMap<>();
        Map<ArenaTeam.RunType, Set<Race>> racesByRunType = new HashMap<>();
        Map<Race, WinRate> winRateByRace = new HashMap<>();
        arenaTeamsByRunType.forEach((runType, arenaTeams) -> {
            racesByRunType.put(runType, arenaTeams.stream().map(ArenaTeam::getRace).collect(Collectors.toSet()));
            teamUuidsByRunType.put(runType,
                    arenaTeams.stream().map(ArenaTeam::getTeamUuid).collect(Collectors.toSet()));

            Map<Race, WinRate> currWinRateByRace = arenaTeams.stream()
                    .filter(team -> Objects.nonNull(team.getResults()))
                    .collect(groupingBy(
                            ArenaTeam::getRace,
                            Collectors.collectingAndThen(
                                    toList(),
                                    ArenaService::calculateWinRate
                            )
                    ));
            mergeWinRate(currWinRateByRace, winRateByRace);
        });
        arenaCoach.setWinRateByRace(winRateByRace);
        arenaCoach.setActiveTeamsCount(ofNullable(teamUuidsByRunType.get(active)).map(Collection::size).orElse(0));
        arenaCoach.setCompletedTeamsCount(teamUuidsByRunType.getOrDefault(completed, emptySet()).size());
        arenaCoach.setFailedTeamsCount(teamUuidsByRunType.getOrDefault(failed, emptySet()).size());
        Set<Race> activeRaces = racesByRunType.getOrDefault(active, emptySet());
        activeRaces.removeAll(racesByRunType.getOrDefault(completed, emptySet()));
        arenaCoach.setCompletedRaces(racesByRunType.getOrDefault(completed, emptySet()));
        arenaCoach.setActiveNotCompletedRacesCount(activeRaces.size());
        List<List<Match>> matches = arenaTeamsByRunType.getOrDefault(completed, emptyList()).stream()
                .map(ArenaTeam::getMatches).toList();
        Optional<Date> lastCompletion = matches
                .stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Match::getFinished)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
        arenaCoach.setLastCompletion(lastCompletion.orElse(null));
        return arenaCoach;
    }

    private static void mergeWinRate(Map<Race, WinRate> currWinRatesByRace, Map<Race, WinRate> winRateByRace) {
        currWinRatesByRace.forEach((race, newWinRate) -> winRateByRace.merge(
                race,
                newWinRate,
                WinRate.merge()
        ));
    }

    private static WinRate calculateWinRate(List<ArenaTeam> teams) {
        int wins = getSum(teams, ResultType.win);
        int draws = getSum(teams, ResultType.draw);
        int losses = getSum(teams, ResultType.loss);

        return new WinRate(wins, draws, losses);
    }

    private static int getSum(List<ArenaTeam> teams, ResultType resultType) {
        return teams.stream()
                .mapToInt(team -> sumUpResults(team.getResults(), resultType))
                .sum();
    }

    private static Integer sumUpResults(List<ArenaTeam.Result> results, ResultType resultType) {
        return results
                .stream()
                .filter(r -> r.getResult() == resultType)
                .mapToInt(ArenaTeam.Result::getCount)
                .sum();
    }

    private void updateLogoAndNameFromContestsData(ArenaTeam arenaTeam) {
        Optional<Team> team = arenaTeam
                .getMatches()
                .stream()
                .max(comparing(Match::getFinished))
                .map(Match::getTeams)
                .orElse(emptyList())
                .stream()
                .filter(t -> t.getId().equals(arenaTeam.getTeamUuid()))
                .findFirst();
        team.ifPresent(t -> {
            arenaTeam.setTeamLogo(t.getLogo());
            arenaTeam.setTeamName(t.getName());
        });
    }

    private boolean matchesRunType(ArenaTeam arenaTeam, ArenaTeam.RunType runType) {
        return runType == getRunTypeFor(arenaTeam);
    }

    private ArenaTeam.RunType getRunTypeFor(ArenaTeam arenaTeam) {
        final AtomicInteger completedCount = new AtomicInteger(0);
        final AtomicInteger failedCount = new AtomicInteger(0);
        final AtomicInteger activeCount = new AtomicInteger(0);
        countRunsInto(arenaTeam.getTeamUuid(), arenaTeam.getMatches(), activeCount, completedCount, failedCount);

        final AtomicInteger completedCountForLast9Games = new AtomicInteger(0);
        final AtomicInteger failedCountForLast9Games = new AtomicInteger(0);
        final AtomicInteger activeCountForLast9Games = new AtomicInteger(0);
        countRunsInto(arenaTeam.getTeamUuid(), arenaTeam.getMatches(), activeCountForLast9Games,
                completedCountForLast9Games, failedCountForLast9Games, true, of(9));

        if (completedCount.get() == 0 && completedCountForLast9Games.get() > 0) {
            completedCount.getAndIncrement();
        }

        if (completedCount.get() > 0) {
            return completed;
        } else if (activeCount.get() > 0) {
            return active;
        } else if (failedCount.get() > 0) {
            return failed;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Could not determine run type for %s (completedCount: %s, activeCount: %s, failedCount: %s).",
                    arenaTeam.getTeamName(), completedCount.get(), activeCount.get(), failedCount.get()));
        }
    }

    private List<ArenaTeam> loadArenaTeamsFor(UUID competitionUuid,
            Race race, Optional<Integer> limit, Optional<Integer> offset) {
        return queryArenaTeamsFor(competitionUuid, race, unpaged());
    }

    Optional<ArenaInfo> toArenaInfo(final Race race, List<ArenaTeam> arenaTeams) {
        if (arenaTeams.isEmpty()) {
            return empty();
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
                            .filter(r -> r.getResult() == ResultType.win)
                            .mapToInt(ArenaTeam.Result::getCount)
                            .sum();
                    winsByTeamId.putIfAbsent(teamUuid, wins);
                    Integer losses = arenaTeam
                            .getResults()
                            .stream()
                            .filter(r -> r.getResult() == ResultType.loss)
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
                            completedCountForLast9Games, failedCountForLast9Games, true, of(9));

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
        return of(arenaInfo);
    }

    private void countRunsInto(UUID teamUuid,
            List<Match> matches,
            AtomicInteger activeCount,
            AtomicInteger completedCount,
            AtomicInteger failedCount) {
        countRunsInto(teamUuid, matches, activeCount, completedCount, failedCount, false, empty());
    }

    private void countRunsInto(UUID teamUuid,
            List<Match> matches,
            AtomicInteger activeCount,
            AtomicInteger completedCount,
            AtomicInteger failedCount,
            boolean goBackwards,
            Optional<Integer> limit) {
        Comparator<Match> comparing = comparing(Match::getFinished);
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
                .max(comparing(Team::getScore))
                .map(Team::getId)
                .orElse(null);
    }

    private Coach toCoach(ArenaTeam arenaTeam) {
        Coach coach = new Coach();
        coach.setName(arenaTeam.getCoachName());
        coach.setId(arenaTeam.getCoachUuid());
        return coach;
    }

    private List<ArenaTeam> queryArenaTeamsFor(UUID competitionUuid,
            Race race, Pageable pageable) {
        return queryArenaTeamsFor(competitionUuid, of(race), empty(), empty(), pageable);
    }

    private List<ArenaTeam> queryArenaTeamsFor(UUID competitionUuid, UUID coachId, Pageable pageable) {
        return queryArenaTeamsFor(competitionUuid, empty(), of(coachId), empty(), pageable);
    }

    private List<ArenaTeam> queryArenaTeamsFor(UUID competitionUuid,
            Optional<Race> race,
            Optional<UUID> coachId,
            Optional<Integer> minWins,
            Pageable pageable) {
        return matchRepository.queryArenaTeamsFor(
                competitionUuid,
                race.orElse(null), coachId.orElse(null), minWins.orElse(null), pageable);
    }

    public ArenaCoachWithArenaTeams loadArenaCoachWithArenaTeams(UUID competitionUuid, UUID coachUuid) {
        Map<ArenaTeam.RunType, List<ArenaTeam>> runTypeListMap = loadArenaTeamsForInternal(competitionUuid, coachUuid);
        Optional<ArenaTeam> first = runTypeListMap
                .values()
                .stream()
                .findFirst()
                .orElse(emptyList())
                .stream()
                .findFirst();
        return first.map(arenaTeam ->
                        new ArenaCoachWithArenaTeams(toArenaCoach(toCoach(arenaTeam), runTypeListMap), runTypeListMap))
                .orElse(new ArenaCoachWithArenaTeams());
    }
}
