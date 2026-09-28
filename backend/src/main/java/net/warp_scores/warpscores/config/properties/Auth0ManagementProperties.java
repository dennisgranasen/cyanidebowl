package net.warp_scores.warpscores.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.auth0-management")
public class Auth0ManagementProperties {
    private String domain;
    private String clientId;
    private String clientSecret;

    public boolean isConfigured() {
        return hasText(domain) && hasText(clientId) && hasText(clientSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}