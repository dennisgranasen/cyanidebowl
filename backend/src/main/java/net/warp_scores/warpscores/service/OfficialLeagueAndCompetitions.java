package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideDefaultsProperties;
import net.warp_scores.warpscores.config.properties.OfficialLeagueProperties;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.CompetitionFormat;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfficialLeagueAndCompetitions {
    private final OfficialLeagueProperties competitionNameProperties;
    private final CyanideDefaultsProperties cyanideDefaultsProperties;

    private static final String ARENA_MODE_COMPETITION_NAME_PATTERN = "ARENA_SEASON_[0-9]+_NAME";

    private boolean isOfficialLeague(Identity leagueId) {
        String key;
        if (SimpleIdentity.class.isAssignableFrom(leagueId.getClass())) {
            SimpleIdentity sid = (SimpleIdentity)leagueId;
            key = sid.getKey();
        }
        else if (CompositeIdentity.class.isAssignableFrom(leagueId.getClass())) {
            CompositeIdentity cid = (CompositeIdentity)leagueId;
            key = cid.asSimpleIdentity(0).getKey().toLowerCase();
        } else {
            return false;
        }
        return Arrays.stream(cyanideDefaultsProperties.getOfficialLeagueIds())
                .map(String::toLowerCase)
                .anyMatch(key::equals);
    }

    public void adjustCompetitionNameAndLogo(Identity leagueId,
            String competitionName,
            Consumer<String> competitionNameConsumer, Consumer<String> competitionLogoConsumer) {
        if (leagueId != null && isOfficialLeague(leagueId)) {
            adjustCompetitionName(leagueId, competitionName, competitionNameConsumer);
            adjustCompetitionLogo(leagueId, competitionName, competitionLogoConsumer);
        }
    }

    public void adjustCompetitionName(Identity leagueId,
            String competitionName,
            Consumer<String> competitionNameConsumer) {
        if (leagueId != null && isOfficialLeague(leagueId)) {
            Optional<String> competitionNameMapping = Optional.ofNullable(
                    competitionNameProperties.getCompetitionName(competitionName));
            competitionNameConsumer.accept(competitionNameMapping.orElse(competitionName));
        }
    }

    public void adjustCompetitionLogo(Identity leagueId,
            String competitionName,
            Consumer<String> competitionLogoConsumer) {
        if (leagueId != null && isOfficialLeague(leagueId)) {
            Optional<String> competitionLogoMapping = Optional.ofNullable(
                    competitionNameProperties.getCompetitionLogo(competitionName));
            competitionLogoMapping.ifPresent(competitionLogoConsumer);
        }
    }

    public void adjustCompetitionFormat(Identity leagueId,
            String competitionName,
            Consumer<CompetitionFormat> competitionFormatConsumer) {
        if (leagueId != null && isOfficialLeague(leagueId) &&
            competitionName.matches(ARENA_MODE_COMPETITION_NAME_PATTERN)) {
            competitionFormatConsumer.accept(CompetitionFormat.Arena);
        }
    }
}
