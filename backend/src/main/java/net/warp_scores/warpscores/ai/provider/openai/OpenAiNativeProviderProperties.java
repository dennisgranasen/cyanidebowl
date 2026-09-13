package net.warp_scores.warpscores.ai.provider.openai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Native OpenAI configuration for capabilities that are not routed through the
 * OpenAI-compatible text provider adapter, such as image generation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.ai.providers.openai")
public class OpenAiNativeProviderProperties {
    private String apiKey;
    private String imageModel = "gpt-image-2";
    private Duration timeout = Duration.ofMinutes(3);
}
