package net.warp_scores.warpscores.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:officialLeagueCompetitionNames.properties")
@RequiredArgsConstructor
public class OfficialLeagueCompetitionNameProperties {

    private final Environment env;

    public String getCompetitionName(String competitionNameKey) {
        return env.containsProperty(competitionNameKey) ? env.getProperty(competitionNameKey) : competitionNameKey;
    }

    public String getCompetitionLogo(String competitionNameKey) {
        String competitionLogoKey = competitionNameKey.replaceAll("_NAME", "_LOGO");
        return env.containsProperty(competitionLogoKey) ? env.getProperty(competitionLogoKey) : null;
    }
}
