package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class TeamAndRaceStats {
    private Map<Team, Stats> teamStats = new HashMap<>();
    private Map<Race, Stats> raceStats = new HashMap<>();

    public void collectInto(Team team, Stats stats)
    {
        teamStats.computeIfAbsent(team,t -> new Stats());
        teamStats.getOrDefault(team, new Stats()).accumulate(stats);
        raceStats.computeIfAbsent(team.getRace(), r -> new Stats());
        raceStats.getOrDefault(team.getRace(), new Stats()).accumulate(stats);
    }
}
