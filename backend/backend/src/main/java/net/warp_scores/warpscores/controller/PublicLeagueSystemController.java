package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.PhaseRepository;
import net.warp_scores.warpscores.model.Phase;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.service.StageMatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PublicLeagueSystemController {

    private final LeagueSystemRepository leagueSystems;
        private final SeasonRepository seasons;
        private final StageRepository stages;
        private final PhaseRepository phases;
        private final StageMatchService stageMatchService;

    @GetMapping("/league-systems")
    public List<LeagueSystemSummary> getLeagueSystems() {
        return leagueSystems.findAll().stream()
                .map(LeagueSystemSummary::from)
                .toList();
    }

                @GetMapping("/league-systems/{leagueSystemId}/overview")
                public LeagueSystemOverview getLeagueSystemOverview(@PathVariable String leagueSystemId,
                        @RequestParam(required = false) String seasonId) {
                var leagueSystem = leagueSystems.findById(leagueSystemId)
                    .orElseThrow(() -> new IllegalArgumentException("League system not found: " + leagueSystemId));
                List<Season> systemSeasons = seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId);
                List<StageWithSeason> systemStages = systemSeasons.stream()
                    .flatMap(season -> stages.findBySeasonIdOrderBySequenceAsc(season.getId()).stream()
                        .map(stage -> new StageWithSeason(season, stage)))
                    .toList();

                Season selectedSeason = seasonId == null
                    ? systemSeasons.stream().max(Comparator
                        .comparing((Season season) -> season.getSequence() == null ? Integer.MIN_VALUE : season.getSequence())
                        .thenComparing(season -> season.getNumber() == null ? Integer.MIN_VALUE : season.getNumber()))
                        .orElse(null)
                    : systemSeasons.stream().filter(season -> season.getId().equals(seasonId)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Season not found in league system: " + seasonId));
                Map<String, List<StageMatchResponse>> matchCache = new HashMap<>();

                List<LeagueSystemOverview.RecentMatch> allRecentMatches = systemStages.stream()
                    .filter(stage -> selectedSeason != null && stage.season().getId().equals(selectedSeason.getId()))
                    .flatMap(stage -> recentMatchesForStage(stage, matchCache))
                    .sorted(Comparator.comparing(
                        recent -> recent.match().finishedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();

                List<LeagueSystemOverview.Season> seasonOverviews = systemSeasons.stream()
                    .map(season -> new LeagueSystemOverview.Season(
                        season.getId(),
                        season.getNumber(),
                        season.getName(),
                        season.getSequence(),
                        phaseOverviews(season, systemStages, matchCache,
                            selectedSeason != null && season.getId().equals(selectedSeason.getId())),
                        systemStages.stream()
                            .filter(stage -> stage.season().getId().equals(season.getId()))
                            .map(StageWithSeason::stage)
                            .map(stage -> overviewStage(stage,
                                selectedSeason != null && season.getId().equals(selectedSeason.getId()), matchCache))
                            .toList(),
                        allRecentMatches.stream()
                            .filter(recent -> recent.seasonId().equals(season.getId()))
                            .limit(12)
                            .toList()))
                    .toList();

                List<LeagueSystemOverview.RecentMatch> recentMatches = allRecentMatches.stream().limit(12).toList();

                return new LeagueSystemOverview(leagueSystem.getId(), leagueSystem.getName(), seasonOverviews, recentMatches);
                }

                public LeagueSystemOverview getLeagueSystemOverview(String leagueSystemId) {
                    return getLeagueSystemOverview(leagueSystemId, null);
                }

                private List<LeagueSystemOverview.Phase> phaseOverviews(Season season, List<StageWithSeason> systemStages,
                        Map<String, List<StageMatchResponse>> matchCache, boolean includeMatches) {
                    List<Phase> seasonPhases = phases.findBySeasonIdOrderBySequenceAsc(season.getId());
                    List<LeagueSystemOverview.Phase> result = seasonPhases.stream().map(phase ->
                            new LeagueSystemOverview.Phase(phase.getId(), phase.getName(),
                                    phase.getType() == null ? null : phase.getType().name(), phase.getSequence(),
                                    systemStages.stream().map(StageWithSeason::stage)
                                            .filter(stage -> phase.getId().equals(stage.getPhaseId()))
                                            .map(stage -> overviewStage(stage, includeMatches, matchCache)).toList())).collect(java.util.stream.Collectors.toList());
                    List<Stage> legacy = systemStages.stream().filter(item -> item.season().getId().equals(season.getId()))
                            .map(StageWithSeason::stage).filter(stage -> stage.getPhaseId() == null).toList();
                    if (!legacy.isEmpty()) result.add(new LeagueSystemOverview.Phase(null, "Stages", "OTHER", 0,
                            legacy.stream().map(stage -> overviewStage(stage, includeMatches, matchCache)).toList()));
                    return result;
                }

                private LeagueSystemOverview.Stage overviewStage(Stage stage, boolean includeMatches,
                        Map<String, List<StageMatchResponse>> matchCache) {
                    return new LeagueSystemOverview.Stage(stage.getId(), stage.getPhaseId(), stage.getName(),
                            stage.getType() == null ? null : stage.getType().name(), stage.getFormat(),
                            stage.getStep(), stage.getDisplayOrder(), includeMatches ? matchesForStage(stage, matchCache) : List.of());
                }

                private List<StageMatchResponse> matchesForStage(Stage stage,
                        Map<String, List<StageMatchResponse>> matchCache) {
                    if (matchCache.containsKey(stage.getId())) return matchCache.get(stage.getId());
                    try {
                        List<StageMatchResponse> result = stageMatchService.getMatchesForStage(stage.getId()).stream()
                            .map(StageMatchResponse::from).toList();
                        matchCache.put(stage.getId(), result);
                        return result;
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        log.warn("Skipping results for misconfigured stage {}: {}", stage.getId(), exception.getMessage());
                        matchCache.put(stage.getId(), List.of());
                        return matchCache.get(stage.getId());
                    }
                }

                private record StageWithSeason(Season season, Stage stage) {
                }

                    private Stream<LeagueSystemOverview.RecentMatch> recentMatchesForStage(StageWithSeason stage,
                            Map<String, List<StageMatchResponse>> matchCache) {
                        try {
                            return matchesForStage(stage.stage(), matchCache).stream()
                                    .filter(match -> match.finishedAt() != null)
                                    .map(match -> new LeagueSystemOverview.RecentMatch(
                                                stage.season().getId(),
                                                stage.stage().getPhaseId(),
                                                phaseName(stage.stage().getPhaseId()),
                                                stage.stage().getId(),
                                                stage.stage().getName(),
                                                match));
                        } catch (IllegalArgumentException | IllegalStateException exception) {
                            log.warn("Skipping results for misconfigured stage {}: {}", stage.stage().getId(), exception.getMessage());
                            return Stream.empty();
                        }
                    }

                    private String phaseName(String phaseId) {
                        return phaseId == null ? null : phases.findById(phaseId).map(Phase::getName).orElse(null);
                    }
}
