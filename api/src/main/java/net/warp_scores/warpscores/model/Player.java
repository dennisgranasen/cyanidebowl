package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
public class Player {
    @Id
    private UUID id;
    private String name;
    private Integer raceId;
    private Integer number;
    private Integer value;
    private Integer xp;
    private Integer level;
    private Attributes attributes;
    private ExtendedAttributes extendedAttributes;
    private String type;
    private Integer[] casualtiesStateIds;
    private String[] casualtiesStates;
    private Boolean suspendedNextMatch;
    private String[] skills;
    private Integer matchplayed;

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
}


