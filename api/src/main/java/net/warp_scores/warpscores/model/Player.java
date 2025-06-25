package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
@ToString(of = {"name", "id"})
public class Player implements Identifiable {
    @Id
    private final Identity id;

    public String getPlayerId() { return id != null ? id.getValue() : null; }

    public Player(Identity id) {
        this.id = id;
    } 

    private String name;
    private Integer raceId;
    private Integer number;
    private Integer value;
    private Integer xp;
    private Integer xpGain;
    private Integer level;
    private Attributes attributes;
    private ExtendedAttributes extendedAttributes;
    private String type;
    private Integer[] casualtiesStateIds;
    private String[] casualtiesState;
    private Boolean suspendedNextMatch;
    private String[] skills;
    private Boolean mvp;
    private Integer matchplayed;
    private Stats stats;

    @Getter
    @Setter
    public static class Attributes {
        private Integer pa;
        private Integer ma;
        private Integer st;
        private Integer ag;
        private Integer av;
    }

    @Getter
    @Setter
    public static class ExtendedAttributes {
        private Attributes defaultAttributes;
        private List<LinkedHashMap<String, Integer>> bonus;
        private List<LinkedHashMap<String, Integer>> malus;
    }

    @Getter
    @Setter
    public static class Stats {
        private Integer spp_gained;
        private Integer touchdowns_scored;
        private Integer yards_running;
        private Integer yards_rushing;
        private Integer hand_off_try;
        private Integer hand_off_success;
        private Integer catch_up_ball_try;
        private Integer catch_up_ball_success;
        private Integer rush_try;
        private Integer rush_success;
        private Integer dodge_try;
        private Integer dodge_success;
        private Integer pick_up_try;
        private Integer pick_up_success;
        private Integer blocks_succeeded;
        private Integer blocks_sustained;
        private Integer blitz_done;
        private Integer armour_breaks;
        private Integer injuries_inflicted;
        private Integer injuries_sustained;
        private Integer stun_inflicted;
        private Integer stun_sustained;
        private Integer ko_inflicted;
        private Integer ko_sustained;
        private Integer casualties_inflicted;
        private Integer casualties_sustained;
        private Integer kills_inflicted;
        private Integer deaths_sustained;
        private Integer foul_done;
        private Integer foul_sustained;
        private Integer throw_team_mate_try;
        private Integer throw_team_mate_success;
    }
}


