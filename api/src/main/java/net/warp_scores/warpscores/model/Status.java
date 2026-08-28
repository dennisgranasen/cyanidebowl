package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.model.Status.Maintenance;
import net.warp_scores.warpscores.model.Status.News;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document
@ToString
@EqualsAndHashCode(of = "gameName")
@NoArgsConstructor
public class Status {


    @Id
    private String gameName;
    private String codename;
    private String title;
    private boolean overall;
    private ServiceStatus[] serviceStatuses;

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
        private List<Object> pc;
        private List<Object> microsoft;
        private List<Object> sony;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class Platform {
        private String codename;
        private String title;
        private boolean ok;
        private Region[] regions;
        private ServiceStatus[] services;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class News {
        private String title;
        private String message;
        private String description;
        private String backgroundImageURL;
        private boolean isBackgroundLocalURL;
        private String urlToRedirect;
        private boolean isRedirectLocalURL;
        private String localURLType;
        private String itemID;   
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode(of = "serviceName")
    public static class ServiceStatus {
        private String serviceName; 
        private Boolean isOk;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class Region {
        private String codename;
        private String title;
        private boolean ok;
        private ServiceStatus[] services;
    }
    
}

