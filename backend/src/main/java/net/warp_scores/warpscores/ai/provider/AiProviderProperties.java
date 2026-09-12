package net.warp_scores.warpscores.ai.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** External configuration for provider/model routing. Credentials remain provider-specific env/config. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.ai")
public class AiProviderProperties {
    private List<ModelTargetConfig> defaultTargets = new ArrayList<>();
    private Map<String, List<ModelTargetConfig>> reporterTargets = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class ModelTargetConfig {
        private String providerId;
        private String model;
    }
}
