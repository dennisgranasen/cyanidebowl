package net.warp_scores.warpscores.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.Map;

@Getter
@With
@NoArgsConstructor
@AllArgsConstructor
public class ArenaCoachWithArenaTeams {
    private ArenaCoach arenaCoach;
    private Map<ArenaTeam.RunType, List<ArenaTeam>> arenaTeams;
}
