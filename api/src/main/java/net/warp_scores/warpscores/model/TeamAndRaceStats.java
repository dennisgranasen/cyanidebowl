package net.warp_scores.warpscores.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Document
public class TeamAndRaceStats {
    @Id
    String _id;

    private Map<String, Stats> teamStats = new HashMap<>();
    private Map<String, Stats> raceStats = new HashMap<>();
    private Map<String, SimpleTeam> teams = new HashMap<>();

    public void collectInto(Team team, Stats stats) {
        String teamId = team.getTeamId();
        String race = team.getRace();
        teamStats.computeIfAbsent(teamId, t -> new Stats());
        teamStats.getOrDefault(teamId, new Stats()).accumulate(stats);
        raceStats.computeIfAbsent(race, r -> new Stats());
        raceStats.getOrDefault(race, new Stats()).accumulate(stats);
        SimpleTeam simpleTeam = toSimpleTeam(team);
        teams.putIfAbsent(teamId, simpleTeam);
    }

    private SimpleTeam toSimpleTeam(Team team) {
        return new SimpleTeam()
                .withTeamId(team.getTeamId())
                .withTeamName(team.getName())
                .withRace(team.getRace())
                .withCoachId(team.getCoachId() != null ? team.getCoachId().getValue() : null)
                .withCoachName(team.getCoachName())
                .withLogo(team.getLogo());
    }

    @Getter
    @Setter
    @With
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(of = "teamId")
    @ToString(of = {"teamId", "teamName", "coachId", "coachName", "race"})
    public static class SimpleTeam {
        private String teamId;
        private String teamName;
        private String coachId;
        private String coachName;
        private String race;
        private String logo;
    }
}
