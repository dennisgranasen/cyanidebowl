package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document
@ToString
@NoArgsConstructor
public class Status implements UpdateableFromApi {
    @Id
    private String gameName;
    private boolean overall;
    private Object serviceStatuses;

    private Platform[] platforms;
    private Maintenance maintenance;
    private News[] news;
    private String[] socialLinks;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheck;

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class Maintenance {
        private Object pc;
        private Object microsoft;
        private Object sony;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class Platform {
        private String codename;
        private String title;
        private boolean ok;
        private Object[] regions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class News {
        private String title;
        private Object message;
    }
}

