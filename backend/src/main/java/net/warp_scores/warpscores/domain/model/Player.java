package net.warp_scores.warpscores.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Document
public class Player implements UpdateableFromApi {
    @Id
    private UUID id;
    private String name;
    private int raceId;
    private int number;
    private int value;
    private int xp;
    private int level;
    private Attributes attributes;
    private ExtendedAttributes extendedAttributes;
    private String type;
    private Integer[] casualtiesStateIds;
    private String[] casualtiesStates;
    private boolean suspendedNextMatch;
    private String[] skills;

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


