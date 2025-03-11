package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.OfficialLeagueCompetitionNameProperties;
import net.warp_scores.warpscores.model.CompetitionFormat;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfficialLeagueAndCompetitions {
    private final OfficialLeagueCompetitionNameProperties competitionNameProperties;

    private static final UUID OFFICIAL_LEAGUE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000025");
    private static final String ARENA_MODE_COMPETITION_NAME_PATTERN = "ARENA_SEASON_[0-9]+_NAME";

    public void adjustCompetitionNameAndLogo(UUID leagueId,
            String competitionName,
            Consumer<String> competitionNameConsumer, Consumer<String> competitionLogoConsumer) {
        adjustCompetitionName(leagueId, competitionName, competitionNameConsumer);
        adjustCompetitionLogo(leagueId, competitionName, competitionLogoConsumer);
    }

    public void adjustCompetitionName(UUID leagueId,
            String competitionName,
            Consumer<String> competitionNameConsumer) {
        if (OFFICIAL_LEAGUE_UUID.equals(leagueId)) {
            Optional<String> competitionNameMapping = Optional.ofNullable(
                    competitionNameProperties.getCompetitionName(competitionName));
            competitionNameConsumer.accept(competitionNameMapping.orElse(competitionName));
        }
    }

    public void adjustCompetitionLogo(UUID leagueId,
            String competitionName,
            Consumer<String> competitionLogoConsumer) {
        if (OFFICIAL_LEAGUE_UUID.equals(leagueId)) {
            Optional<String> competitionLogoMapping = Optional.ofNullable(
                    competitionNameProperties.getCompetitionLogo(competitionName));
            competitionLogoMapping.ifPresent(competitionLogoConsumer);
        }
    }

    public void adjustCompetitionFormat(UUID leagueId,
            String competitionName,
            Consumer<CompetitionFormat> competitionFormatConsumer) {
        if (OFFICIAL_LEAGUE_UUID.equals(leagueId) && competitionName.matches(ARENA_MODE_COMPETITION_NAME_PATTERN)) {
            competitionFormatConsumer.accept(CompetitionFormat.Arena);
        }
    }
}
