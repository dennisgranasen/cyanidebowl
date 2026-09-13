package net.warp_scores.warpscores.ai.provider.cloudflare;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.ai.providers.cloudflare")
public class CloudflareAiProviderProperties {
    private String accountId;
    private String apiKey;
    private String imageModel = "@cf/black-forest-labs/flux-1-schnell";
    private Duration timeout = Duration.ofMinutes(3);
    private int imageSteps = 4;
}
