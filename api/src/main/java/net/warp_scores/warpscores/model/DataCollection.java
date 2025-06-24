package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"collectionType", "id", "dateLastCollectedInfo"})
public class DataCollection implements Identifiable {
    @Id
    private final Identity id;
    private final EntityType collectionType;

    public DataCollection(Identity id, EntityType collectionType) {
        this.id = id;
        this.collectionType = collectionType;
    }

    //private Integer oldLeagueId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastCollectedInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastCollectedMatches;

}
