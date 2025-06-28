package net.warp_scores.warpscores.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cyanide.defaults")
public class CyanideDefaultsProperties {
    private String[] officialLeagueIds;

    public String[] getOfficialLeagueIds() {
        return officialLeagueIds;
    }

    public void setOfficialLeagueIds(String[] officialLeagueIds) {
        this.officialLeagueIds = officialLeagueIds;
    }
}