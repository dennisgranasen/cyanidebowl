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
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "collectionType"})
public class TeamCollection implements Identifiable{
    @Id
    private final Identity id;
    private final EntityType collectionType;

    public TeamCollection(Identity id, EntityType collectionType) {
        this.id = id;
        this.collectionType = collectionType;
    }

    private List<String> teamIds;
}
