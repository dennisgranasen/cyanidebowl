package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "identity")
@ToString(of = {"identity", "collectionType"})
public class TeamCollection {
    @Id
    private final Identity identity;
    private final EntityType collectionType;

    public TeamCollection(Identity identity, EntityType collectionType) {
        this.identity = identity;
        this.collectionType = collectionType;
    }

    private List<String> teamIds;
}
