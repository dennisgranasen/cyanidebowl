package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
public class Coach {
    @Id
    private UUID id;
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

    @Override
    public String toString() {
            return String.format("Coach[%s] %s", id, name);
    }
}
