package net.warp_scores.warpscores.controller;

import java.util.List;

public record LeagueSystemOverview(
        String id,
        String name,
        List<Season> seasons,
        List<RecentMatch> recentMatches) {

        public record Season(
            String id,
            Integer number,
            String name,
            Integer sequence,
            List<Stage> stages,
            List<RecentMatch> recentMatches) {
    }

    public record Stage(String id, String phase, String name, String format) {
    }

    public record RecentMatch(String seasonId, String stageId, String stageName, StageMatchResponse match) {
    }
}
