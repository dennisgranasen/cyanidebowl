package net.warp_scores.warpscores.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CyanideApiProperties {

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

