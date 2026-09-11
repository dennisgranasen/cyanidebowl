package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * LeagueSystem-wide relationship history for coaches, teams and races.
 */
@Value
@Builder
public class RivalryContext {
    MatchupHistory coachVsCoach;
    MatchupHistory teamVsTeam;
    MatchupHistory raceVsRace;

    @Singular("extension")
    Map<String, Object> extensions;

    @Value
    @Builder
    public static class MatchupHistory {
        String subjectAId;
        String subjectAName;
        String subjectBId;
        String subjectBName;
        Integer meetings;
        Integer winsA;
        Integer draws;
        Integer winsB;
        Integer touchdownsA;
        Integer touchdownsB;
        Integer casualtiesA;
        Integer casualtiesB;
        Double rivalryScore;
        List<Map<String, Object>> notableMeetings;

        @Singular
        List<String> signals;

        @Singular("extension")
        Map<String, Object> extensions;
    }
}
