package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.StageSource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeagueSystemDiscoveryServiceTest {
    private final MatchRepository matches = mock(MatchRepository.class);
    private final StageSourceRepository sources = mock(StageSourceRepository.class);
    private final LeagueSystemDiscoveryService service = new LeagueSystemDiscoveryService(matches, sources);

    @Test
    void suggestsUnconfiguredCompetitionAndParsesRomanSeason() {
        LeagueSystem system = system(List.of("Nuffle Spitfire", "NST"));
        var competitionId = new CompositeIdentity(2, "league", "competition-24");
        var leagueId = new CompositeIdentity(2, "league");
        when(sources.findByLeagueSystemId("nst")).thenReturn(List.of());
        when(matches.findSourceDiscoveryRecords()).thenReturn(List.of(
                new MatchRepository.SourceDiscoveryRecord(
                        competitionId, leagueId, "NST XXIV", "Nuffle Spitfire Trophy 24",
                        "pc", new Date(1000), 12)));

        var candidates = service.discover(system);

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.sourceEntityId()).isEqualTo(competitionId.asMongoKey());
            assertThat(candidate.suggestedSeasonNumber()).isEqualTo(24);
            assertThat(candidate.game()).isEqualTo(GameType.BB2);
            assertThat(candidate.matchCount()).isEqualTo(12);
        });
    }

    @Test
    void excludesAlreadyConfiguredAndUnrelatedSources() {
        LeagueSystem system = system(List.of("Nuffle Spitfire"));
        var configuredId = new CompositeIdentity(2, "league", "configured");
        StageSource configured = new StageSource();
        configured.setSourceEntityId(configuredId);
        when(sources.findByLeagueSystemId("nst")).thenReturn(List.of(configured));
        when(matches.findSourceDiscoveryRecords()).thenReturn(List.of(
                new MatchRepository.SourceDiscoveryRecord(configuredId, new CompositeIdentity(2, "league"),
                        "NST 23", "Nuffle Spitfire Trophy", "pc", new Date(), 3),
                new MatchRepository.SourceDiscoveryRecord(new CompositeIdentity(3, "other", "competition"),
                        new CompositeIdentity(3, "other"), "Other 25", "Other League", "cross", new Date(), 2)));

        assertThat(service.discover(system)).isEmpty();
    }

    @Test
    void discoveryRequiresExplicitAliases() {
        assertThat(service.discover(system(List.of()))).isEmpty();
    }

    private LeagueSystem system(List<String> aliases) {
        LeagueSystem system = new LeagueSystem();
        system.setId("nst");
        system.setDiscoveryAliases(aliases);
        return system;
    }
}
