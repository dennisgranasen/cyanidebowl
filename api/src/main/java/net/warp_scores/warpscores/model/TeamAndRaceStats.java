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

    private Map<UUID, Stats> teamStats = new HashMap<>();
    private Map<Race, Stats> raceStats = new HashMap<>();
    private Map<UUID, SimpleTeam> teams = new HashMap<>();

    public void collectInto(Team team, Stats stats) {
        teamStats.computeIfAbsent(team.getId(), t -> new Stats());
        teamStats.getOrDefault(team.getId(), new Stats()).accumulate(stats);
        raceStats.computeIfAbsent(team.getRace(), r -> new Stats());
        raceStats.getOrDefault(team.getRace(), new Stats()).accumulate(stats);
        SimpleTeam simpleTeam = toSimpleTeam(team);
        teams.putIfAbsent(team.getId(), simpleTeam);
    }

    private SimpleTeam toSimpleTeam(Team team) {
        return new SimpleTeam()
                .withTeamUuid(team.getId())
                .withTeamName(team.getName())
                .withRace(team.getRace())
                .withCoachId(team.getCoachId())
                .withCoachName(team.getCoachName())
                .withLogo(team.getLogo());
    }

    @Getter
    @Setter
    @With
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(of = "teamUuid")
    @ToString(of = {"teamUuid", "teamName", "coachId", "coachName", "race"})
    public static class SimpleTeam {
        private UUID teamUuid;
        private String teamName;
        private String coachId;
        private String coachName;
        private Race race;
        private String logo;
    }
}
