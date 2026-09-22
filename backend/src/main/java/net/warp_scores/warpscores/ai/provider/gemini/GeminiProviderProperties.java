package net.warp_scores.warpscores.ai.provider.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.ai.providers.gemini")
public class GeminiProviderProperties {
    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private Duration timeout = Duration.ofSeconds(60);
    private Integer contextWindowTokens;
    private Integer maxOutputTokens;

    public String interactionsUrl() {
        return baseUrl.replaceAll("/+$", "") + "/interactions";
    }
}
