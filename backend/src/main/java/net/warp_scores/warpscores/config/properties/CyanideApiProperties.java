package net.warp_scores.warpscores.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cyanide")
@Getter
@Setter
public class CyanideApiProperties {

    private Defaults defaults;
    private ApiConfig apiConfig;
    private RequestLimit requestLimit;
    private Cache imagesCache;
    private Urls urls;
    private boolean jobCreationSchedulerActive;
    private boolean jobExecutionSchedulerActive;
    private boolean fetchActive;
    private boolean respectApiStatus;
    private String checkApiStatusCron;


    @Getter
    @Setter
    public static class Defaults {
        private Integer opus;
    }

    @Getter
    @Setter
    public static class ApiConfig {
        private String baseUrl;
        private String key;
    }

    @Getter
    @Setter
    public static class RequestLimit {
        private long capacity;
        private long periodInSeconds;
    }

    @Getter
    @Setter
    public static class Cache {
        private long maxValidityInMinutes;
    }

    @Getter
    @Setter
    public static class Urls {
        private Images images;
        private String imagesExtension;
    }

    @Getter
    @Setter
    public static class Images {
        private String logos;
        private String races;
        private String portraits;
        private String skills;
        private String stadiums;
    }
}

