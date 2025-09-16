package net.warp_scores.warpscores.model;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

@Getter
@Setter
@Document
@ToString(of = {"entityId", "legType", "excludes", "includes"})
@EqualsAndHashCode(of = "entityId")
public class CircuitLegEntity implements Comparable<CircuitLegEntity> {
    private Identity entityId; // league or competition id
    @JsonAlias("entityType")
    private EntityType legType;
    private List<Object> excludes;
    private List<Object> includes;
    private String[] entityNames;
    private GameType game;
    private Platform platform;
    private String ruleset;
    private Boolean isArchived;
    private LadderOption ladderOption;

    @Override
    public int compareTo(CircuitLegEntity other) {
        int result;
        result = entityId.compareTo(other.getEntityId());
        if (result != 0) {
            return result;
        }
        result = legType.compareTo(other.getLegType());
        if (result != 0) {
            return result;
        }
        result = game.compareTo(other.getGame());
        if (result != 0) {
            return result;
        }
        result = platform.compareTo(other.getPlatform());
        if (result != 0) {
            return result;
        }
        result = Arrays.compare(
            excludes == null ? null : excludes.toArray(new String[0]),
            other.getExcludes() == null ? null : other.getExcludes().toArray(new String[0])
        );
        if (result != 0) {
            return result;
        }
        result = Arrays.compare(
            includes == null ? null : includes.toArray(new String[0]),
            other.getIncludes() == null ? null : other.getIncludes().toArray(new String[0])
        );
        return result;
        
    }
}

