package net.warp_scores.warpscores.identity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for entities with a MongoDB id.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleIdentity.class, name = "simple"),
    @JsonSubTypes.Type(value = CompositeIdentity.class, name = "composite")
})
public interface Identity {
    String DELIMITER = "_";
    //String getId();
    int getOpus();
    String getValue();
    String asMongoKey();
    String getKey();
}