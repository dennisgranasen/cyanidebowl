package net.warp_scores.warpscores.service;

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
import static net.warp_scores.warpscores.DurationLogger.executeLoggingDuration;
import static net.warp_scores.warpscores.model.ArenaTeam.RunType.active;
import static net.warp_scores.warpscores.model.ArenaTeam.RunType.completed;
import static net.warp_scores.warpscores.model.ArenaTeam.RunType.failed;
import static org.springframework.data.domain.Pageable.unpaged;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ArenaService {
    @Value("${cyanide.defaults.topCoaches:6}")
    private int defaultTopCoaches;

    private static final int NEEDED_WINS_FOR_COMPLETION = 7;
    private static final int LOSSES_BEFORE_ELIMINATION = 2;

    private final MatchRepository matchRepository;

    @Cacheable(ARENA_RACES)
    @DurationLogging
    public List<Race> loadArenaRacesFor(
            Identity competitionId) {
        return matchRepository.getUsedRacesForCompetition(competitionId);
    }

    @Cacheable(ARENA_INFOS)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public Optional<ArenaInfo> loadArenaInfoFor(
            Identity competitionId,
            Race race
            ) {
        List<ArenaTeam> arenaTeams = loadArenaTeamsFor(competitionId, race, empty(), empty());
        return toArenaInfo(race, arenaTeams);
    }

    @Cacheable(ARENA_TEAMS)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public Map<ArenaTeam.RunType, List<ArenaTeam>> loadArenaTeamsFor(Identity competitionId,
            Race race,
            ArenaTeam.RunType runType,
            Optional<Integer> limit,
            Optional<Integer> offset) {
        List<ArenaTeam> teamsForRace = queryArenaTeamsFor(
            competitionId, race, unpaged());
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
    public Map<ArenaTeam.RunType, List<ArenaTeam>> loadArenaTeamsFor(
            Identity competitionId,
            Identity coachId) {
        return loadArenaTeamsForInternal(competitionId, coachId);
    }

    private Map<ArenaTeam.RunType, List<ArenaTeam>> loadArenaTeamsForInternal(
            Identity competitionId,
            Identity coachId) {
        List<ArenaTeam> arenaTeams = queryArenaTeamsFor(competitionId, coachId, unpaged());

        List<ArenaTeam> coachTeams = arenaTeams
                .stream()
                .filter(arenaTeam -> {
                    Identity arenaCoachId = arenaTeam.getCoachId();
                    return arenaCoachId != null && arenaCoachId.getValue().equals(coachId.getValue());
                })
                .sorted(comparing(this::latestFinishedGame).reversed())
                .toList();
        return toArenaTeamsByRunType(coachTeams);
    }

    private Map<ArenaTeam.RunType, List<ArenaTeam>> toArenaTeamsByRunType(
            List<ArenaTeam> coachTeams) {
        coachTeams
                .forEach(this::updateLogoAndNameFromContestsData);
        return coachTeams
                .stream()
                .collect(groupingBy(this::getRunTypeFor, toList()));
    }

    @Cacheable(ARENA_COACHES)
    @DurationLogging(warnThresholdMillis = 1500, errorThresholdMillis = 3000)
    public List<ArenaCoach> loadArenaTopCoachesFor(
            Identity competitionId,
            Optional<Integer> topLimit) {
        List<ArenaTeam> arenaTeams = new ArrayList<>();
        List<Race> races = loadArenaRacesFor(competitionId);
        for (Race race : races) {
            List<ArenaTeam> currQueryArenaTeams = queryArenaTeamsFor(
                competitionId, Optional.of(race), empty(), of(7), unpaged());
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
                .limit(topLimit.orElse(defaultTopCoaches))
                .toList();
    }

    private ArenaCoach toArenaCoach(Coach coach, Map<ArenaTeam.RunType, List<ArenaTeam>> arenaTeamsByRunType) {
        ArenaCoach arenaCoach = new ArenaCoach();
        arenaCoach.setCoachName(coach.getName());
        arenaCoach.setId(coach.getIdentity());

        Map<ArenaTeam.RunType, Set<Identity>> teamUuidsByRunType = new HashMap<>();
        Map<ArenaTeam.RunType, Set<Race>> racesByRunType = new HashMap<>();
        Map<Race, WinRate> winRateByRace = new HashMap<>();
        arenaTeamsByRunType.forEach((runType, arenaTeams) -> {
            racesByRunType.put(runType, arenaTeams.stream().map(ArenaTeam::getRace).collect(Collectors.toSet()));
            teamUuidsByRunType.put(runType,
                    arenaTeams.stream().map(ArenaTeam::getTeamId).collect(Collectors.toSet()));

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

    private static void mergeWinRate(
            Map<Race, WinRate> currWinRatesByRace,
            Map<Race, WinRate> winRateByRace) {
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
                .filter(t -> t.getIdentity().equals(arenaTeam.getTeamId()))
                .findFirst();
        team.ifPresent(t -> {
            arenaTeam.setTeamLogo(t.getLogo());
            arenaTeam.setTeamName(t.getName());
        });
    }

    private boolean matchesRunType(
            ArenaTeam arenaTeam,
            ArenaTeam.RunType runType) {
        return runType == getRunTypeFor(arenaTeam);
    }

    private ArenaTeam.RunType getRunTypeFor(
            ArenaTeam arenaTeam) {
        final AtomicInteger completedCount = new AtomicInteger(0);
        final AtomicInteger failedCount = new AtomicInteger(0);
        final AtomicInteger activeCount = new AtomicInteger(0);
        countRunsInto(arenaTeam.getTeamId(), arenaTeam.getMatches(), activeCount, completedCount, failedCount);

        final AtomicInteger completedCountForLast9Games = new AtomicInteger(0);
        final AtomicInteger failedCountForLast9Games = new AtomicInteger(0);
        final AtomicInteger activeCountForLast9Games = new AtomicInteger(0);
        countRunsInto(arenaTeam.getTeamId(), arenaTeam.getMatches(), activeCountForLast9Games,
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

    @DurationLogging(infoThresholdMillis = 0, warnThresholdMillis = 100, errorThresholdMillis = 500)
    private List<ArenaTeam> loadArenaTeamsFor(Identity competitionId,
            Race race, Optional<Integer> limit, Optional<Integer> offset) {
        return queryArenaTeamsFor(competitionId, race, unpaged());
    }

    @DurationLogging(infoThresholdMillis = 0, warnThresholdMillis = 100, errorThresholdMillis = 500)
    Optional<ArenaInfo> toArenaInfo(final Race race, List<ArenaTeam> arenaTeams) {
        if (arenaTeams.isEmpty()) {
            return empty();
        }
        Set<Identity> coachIds = new HashSet<>();
        Set<Identity> teamIds = new HashSet<>();
        Set<String> matchIds = new HashSet<>();
        Map<Identity, Integer> winsByTeamId = new HashMap<>();
        Map<Identity, Integer> lossesByTeamId = new HashMap<>();
        Map<Identity, Integer> failedRunsByTeamId = new HashMap<>();
        Map<Identity, Integer> completedRunsByTeamId = new HashMap<>();
        Map<Identity, Integer> activeRunsByTeamId = new HashMap<>();
        arenaTeams
                .stream()
                .filter(arenaTeam -> race.equals(arenaTeam.getRace()))
                .forEach(arenaTeam -> {
                    coachIds.add(arenaTeam.getCoachId());
                    matchIds.addAll(
                            arenaTeam
                                    .getMatches()
                                    .stream()
                                    .filter(c -> c.getTeams().stream().map(Team::getRace)
                                            .anyMatch(r -> r.equals(race)))
                                    .map(Match::getMatchId)
                                    .collect(Collectors.toSet()));
                    Identity teamId = arenaTeam.getTeamId();
                    teamIds.add(teamId);
                    Integer wins = arenaTeam
                            .getResults()
                            .stream()
                            .filter(r -> r.getResult() == ResultType.win)
                            .mapToInt(ArenaTeam.Result::getCount)
                            .sum();
                    winsByTeamId.putIfAbsent(teamId, wins);
                    Integer losses = arenaTeam
                            .getResults()
                            .stream()
                            .filter(r -> r.getResult() == ResultType.loss)
                            .mapToInt(ArenaTeam.Result::getCount)
                            .sum();
                    lossesByTeamId.putIfAbsent(teamId, losses);

                    final AtomicInteger completedCount = new AtomicInteger(0);
                    final AtomicInteger failedCount = new AtomicInteger(0);
                    final AtomicInteger activeCount = new AtomicInteger(0);
                    countRunsInto(teamId, arenaTeam.getMatches(), activeCount, completedCount, failedCount);

                    final AtomicInteger completedCountForLast9Games = new AtomicInteger(0);
                    final AtomicInteger failedCountForLast9Games = new AtomicInteger(0);
                    final AtomicInteger activeCountForLast9Games = new AtomicInteger(0);
                    countRunsInto(teamId, arenaTeam.getMatches(), activeCountForLast9Games,
                            completedCountForLast9Games, failedCountForLast9Games, true, of(9));

                    if (completedCount.get() == 0 && completedCountForLast9Games.get() > 0) {
                        completedCount.getAndIncrement();
                    }
                    completedRunsByTeamId.putIfAbsent(teamId, completedCount.get());
                    failedRunsByTeamId.putIfAbsent(teamId, failedCount.get());
                    activeRunsByTeamId.putIfAbsent(teamId, activeCount.get());
                });
        ArenaInfo arenaInfo = new ArenaInfo()
                .withRace(race)
                .withCoaches(coachIds.size())
                .withTeams(teamIds.size())
                .withMatches(matchIds.size())
                .withWins(winsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withLosses(lossesByTeamId.values().stream().mapToInt(l -> l).sum())
                .withActiveRuns(activeRunsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withCompletedRuns(completedRunsByTeamId.values().stream().mapToInt(l -> l).sum())
                .withFailedRuns(failedRunsByTeamId.values().stream().mapToInt(l -> l).sum());
        return of(arenaInfo);
    }

    private void countRunsInto(Identity teamId,
            List<Match> matches,
            AtomicInteger activeCount,
            AtomicInteger completedCount,
            AtomicInteger failedCount) {
        countRunsInto(teamId, matches, activeCount, completedCount, failedCount, false,
            empty());
    }

    private void countRunsInto(Identity teamId,
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
                .forEach(match -> countContestInto(teamId, match, wins, losses,
                     completedCount, failedCount));
        if (wins.get() > 0 || losses.get() > 0) {
            activeCount.getAndIncrement();
        }
    }

    private void countContestInto(Identity teamId, Match match,
            AtomicInteger currWins,
            AtomicInteger currLosses,
            AtomicInteger completedCount,
            AtomicInteger failedCount) {
        Identity winnerId = getWinnerTeamIdOrNull(match);
        if (winnerId == null) {
            return;
        }
        if (teamId.equals(winnerId)) {
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

    private Identity getWinnerTeamIdOrNull(Match match) {
        return match
                .getTeams()
                .stream()
                .max(comparing(Team::getScore))
                .map(Team::getIdentity)
                .orElse(null);
    }

    private Coach toCoach(ArenaTeam arenaTeam) {
        // Assuming ArenaTeam has getCoachIdentity() returning Identity
        return new Coach(arenaTeam.getCoachId());
    }

    private List<ArenaTeam> queryArenaTeamsFor(Identity competitionId,
            Race race, Pageable pageable) {
        return queryArenaTeamsFor(competitionId, of(race), empty(), empty(), pageable);
    }

    private List<ArenaTeam> queryArenaTeamsFor(
            Identity competitionId,
            Identity coachId,
            Pageable pageable){
        return queryArenaTeamsFor(competitionId, empty(), of(coachId), empty(), pageable);
    }

    private List<ArenaTeam> queryArenaTeamsFor(Identity competitionId,
            Optional<Race> race,
            Optional<Identity> coachId,
            Optional<Integer> minWins,
            Pageable pageable) {
        return executeLoggingDuration(() -> matchRepository.queryArenaTeamsFor(
                competitionId, race.orElse(null),
                coachId.map(Identity::getValue).orElse(null),
                minWins.orElse(null), pageable
            ));
    }

    public ArenaCoachWithArenaTeams loadArenaCoachWithArenaTeams(
            Identity competitionId,
            Identity coachId) {
        Map<ArenaTeam.RunType, List<ArenaTeam>> runTypeListMap = loadArenaTeamsForInternal(competitionId, coachId);
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
