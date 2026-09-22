package net.warp_scores.warpscores.ai.provider.openai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configures logical providers that implement the OpenAI Responses API.
 *
 * <p>The map key is the provider id used by routing, for example {@code grok} or
 * {@code openrouter}. Multiple instances share one adapter implementation.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.ai.providers")
public class OpenAiCompatibleProviderProperties {
    private Map<String, Endpoint> openaiCompatible = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Endpoint {
        private String baseUrl;
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean structuredOutput = true;
        private Integer contextWindowTokens;
        private Integer maxOutputTokens;

        public String responsesUrl() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalStateException("OpenAI-compatible baseUrl is required");
            }
            return baseUrl.replaceAll("/+$", "") + "/responses";
        }
    }
}
