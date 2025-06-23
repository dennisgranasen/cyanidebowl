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
@EqualsAndHashCode(of = {"identity"})
@ToString(of = {"collectionType", "identity", "dateLastCollectedInfo"})
public class DataCollection {
    @Id
    private final Identity identity;
    private final EntityType collectionType;

    public DataCollection(Identity identity, EntityType collectionType) {
        this.identity = identity;
        this.collectionType = collectionType;
    }

    //private Integer oldLeagueId;
    
    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastCollectedInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastCollectedMatches;

    private Boolean collectionActive;

    private String platform;    

}
