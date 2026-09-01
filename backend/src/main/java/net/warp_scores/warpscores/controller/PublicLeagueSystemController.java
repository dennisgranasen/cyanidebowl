package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.service.StageMatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PublicLeagueSystemController {

    private final LeagueSystemRepository leagueSystems;
        private final SeasonRepository seasons;
        private final StageRepository stages;
        private final StageMatchService stageMatchService;

    @GetMapping("/league-systems")
    public List<LeagueSystemSummary> getLeagueSystems() {
        return leagueSystems.findAll().stream()
                .map(LeagueSystemSummary::from)
                .toList();
    }

                @GetMapping("/league-systems/{leagueSystemId}/overview")
                public LeagueSystemOverview getLeagueSystemOverview(@PathVariable String leagueSystemId) {
                var leagueSystem = leagueSystems.findById(leagueSystemId)
                    .orElseThrow(() -> new IllegalArgumentException("League system not found: " + leagueSystemId));
                List<Season> systemSeasons = seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId);
                List<StageWithSeason> systemStages = systemSeasons.stream()
                    .flatMap(season -> stages.findBySeasonIdOrderBySequenceAsc(season.getId()).stream()
                        .map(stage -> new StageWithSeason(season, stage)))
                    .toList();

                List<LeagueSystemOverview.RecentMatch> allRecentMatches = systemStages.stream()
                    .flatMap(this::recentMatchesForStage)
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
                        systemStages.stream()
                            .filter(stage -> stage.season().getId().equals(season.getId()))
                            .map(StageWithSeason::stage)
                            .map(stage -> new LeagueSystemOverview.Stage(
                                stage.getId(), stage.getPhase(), stage.getName(), stage.getFormat()))
                            .toList(),
                        allRecentMatches.stream()
                            .filter(recent -> recent.seasonId().equals(season.getId()))
                            .limit(12)
                            .toList()))
                    .toList();

                List<LeagueSystemOverview.RecentMatch> recentMatches = allRecentMatches.stream().limit(12).toList();

                return new LeagueSystemOverview(leagueSystem.getId(), leagueSystem.getName(), seasonOverviews, recentMatches);
                }

                private record StageWithSeason(Season season, Stage stage) {
                }

                    private Stream<LeagueSystemOverview.RecentMatch> recentMatchesForStage(StageWithSeason stage) {
                        try {
                            return stageMatchService.getMatchesForStage(stage.stage().getId()).stream()
                                    .filter(match -> match.finishedAt() != null)
                                    .map(match -> new LeagueSystemOverview.RecentMatch(
                                                stage.season().getId(),
                                                stage.stage().getId(),
                                                stage.stage().getName(),
                                                StageMatchResponse.from(match)));
                        } catch (IllegalArgumentException | IllegalStateException exception) {
                            log.warn("Skipping results for misconfigured stage {}: {}", stage.stage().getId(), exception.getMessage());
                            return Stream.empty();
                        }
                    }
}
