package net.warp_scores.warpscores.controller;

import java.util.List;

public final class StatisticsResponse {
    private StatisticsResponse() {}

    public record Dashboard(String leagueSystemId, String seasonId, int matches, int matchesWithPlayerData,
            List<String> editions, List<Category<PlayerEntry>> players, List<Category<TeamEntry>> teams) {}

    public record Marathon(String leagueSystemId, String edition, boolean mergeTeamsByName, int matches,
            List<String> availableEditions, List<String> comparablePlayerCategories,
            Page<TeamEntry> teams, List<Category<PlayerEntry>> players) {}

    public record Personal(String leagueSystemId, List<String> coachIds, int matches,
            List<Category<PlayerEntry>> players, List<Category<TeamEntry>> teams, List<CoachVersus> versus) {}

    public record Category<T>(String key, String label, List<T> entries) {}
    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record PlayerEntry(String playerId, String name, String teamId, String teamName, String coachId,
            String coachName, String race, Integer raceId, Integer opus, String position, Integer playerValue,
            int games, int spp, List<String> skills, int value) {}

    public record TeamEntry(String teamId, String name, String coachId, String coachName, String race,
            Integer raceId, List<String> editions, int seasons, int games, int wins, int draws, int losses,
            int points, int touchdownsFor, int touchdownsAgainst, int casualtiesFor, int casualtiesAgainst,
            double winPercentage, int value) {}

    public record CoachVersus(String coachId, String coachName, int games, int wins, int draws, int losses,
            int touchdownsFor, int touchdownsAgainst, int casualtiesFor, int casualtiesAgainst) {}
}
