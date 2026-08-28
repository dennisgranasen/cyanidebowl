package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ApiCoach {
    @JsonAlias({"idcoach"})
    private String id;
    @JsonAlias({"coachname"})
    private String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    private String lastlang;

    public enum Status {Registered}

    private String email;
    private String platform;
    @JsonAlias({"oldrating"})
    private Long oldRating;
    @JsonAlias({"newrating"})
    private Long newRating;
    private String twitch;
    private String youtube;
    private String country;
    private String lang;
    private Status status;

    @JsonAlias({"coachcyanearned"})
    private String cyanEarned;
    @JsonAlias({"coachxpearned"})
    private String xpEarned;

    @JsonSetter("id")
    public void setId(Object id) { this.id = id == null ? null : id.toString(); }    
}
