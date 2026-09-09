package net.warp_scores.warpscores.ai.reporting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "warpscores.ai-reporting")
public class AiReportingProperties {
    private boolean enabled = false;
    private double secondReportProbability = 0.10;
    private int interactionDelaySeconds = 30;
    private int maxInitialAiComments = 2;
    private int maximumThreadDepth = 4;
    private String promptVersion = "v1";
    private String narrativeFactsVersion = "v1";
}
