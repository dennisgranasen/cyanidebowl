package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
public class Coach implements Identifiable{
    @Id
    private final Identity id;

    public String getCoachId() {
        return id != null ? id.getValue() : null;
    }

    private String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    private String email;
    private String twitch;
    private String youtube;
    private String country;
    private String lang;
    private String lastLang;
    private String status;
    private Boolean matchValidation;

    public Coach(Identity id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("Coach[%s] %s", getCoachId(), name);
    }
}
