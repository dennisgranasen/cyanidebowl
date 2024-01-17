package de.dbbcev.dbbcbb3facade.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private Attributes attributes;
    private String type;
    private Integer[] casualtiesStateIds;
    private String[] casualtiesStates;
    private boolean suspendedNextMatch;
    private String[] skills;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {
        private Integer pa;
        private Integer ma;
        private Integer st;
        private Integer ag;
        private Integer av;
    }
}
