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
            List<Phase> phases,
            List<Stage> stages,
            List<RecentMatch> recentMatches) {
    }

    public record Phase(String id, String name, String type, Integer sequence, List<Stage> stages) {
    }

    public record Stage(String id, String phaseId, String name, String type, String format,
                        Integer step, Integer displayOrder) {
    }

    public record RecentMatch(String seasonId, String phaseId, String phaseName,
                              String stageId, String stageName, StageMatchResponse match) {
    }
}
