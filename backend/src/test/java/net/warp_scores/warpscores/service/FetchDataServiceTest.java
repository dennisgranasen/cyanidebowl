package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.DataCollectionRepository;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.DataCollection;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FetchDataServiceTest {

    @Mock
    private CyanideApiProperties cyanideApiProperties;
    @Mock
    private CyanideApiService cyanideApiService;
    @Mock
    private DataCollectionRepository dataCollectionRepository;
    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private MatchDomainService matchDomainService;
    @Mock
    private ContestRepository contestRepository;

    private FetchDataService service;

    @BeforeEach
    void setUp() {
        service = new FetchDataService();
        ReflectionTestUtils.setField(service, "cyanideApiProperties", cyanideApiProperties);
        ReflectionTestUtils.setField(service, "cyanideApiService", cyanideApiService);
        ReflectionTestUtils.setField(service, "dataCollectionRepository", dataCollectionRepository);
        ReflectionTestUtils.setField(service, "competitionRepository", competitionRepository);
        ReflectionTestUtils.setField(service, "matchDomainService", matchDomainService);
        ReflectionTestUtils.setField(service, "contestRepository", contestRepository);
        ReflectionTestUtils.setField(service, "defaultFetchMatchMaxAgeLimit", 7);
    }

    @Test
    void schedulerDisabledSkipsContestCollection() {
        when(cyanideApiProperties.isJobCreationSchedulerActive()).thenReturn(false);

        service.fetchCompetitionContests();

        verifyNoInteractions(dataCollectionRepository, competitionRepository, matchDomainService,
                contestRepository, cyanideApiService);
    }

    @Test
    void inProgressCompetitionLoadsContests() {
        Competition competition = competition("in-progress", CompetitionStatus.InProgress);
        givenCompetitionForCollection(competition, Optional.empty(), 0);

        service.fetchCompetitionContests();

        verify(cyanideApiService).loadContests(competition);
    }

    @Test
    void competitionWithLiveContestLoadsContests() {
        Competition competition = competition("live", CompetitionStatus.Finished);
        givenCompetitionForCollection(competition, Optional.empty(), 1);

        service.fetchCompetitionContests();

        verify(cyanideApiService).loadContests(competition);
    }

    @Test
    void recentlyPlayedCompetitionLoadsContests() {
        Competition competition = competition("recent", CompetitionStatus.Finished);
        Date recentMatch = Date.from(Instant.now().minus(Duration.ofDays(2)));
        givenCompetitionForCollection(competition, Optional.of(recentMatch), 0);

        service.fetchCompetitionContests();

        verify(cyanideApiService).loadContests(competition);
    }

    @Test
    void staleInactiveCompetitionDoesNotCallCyanide() {
        Competition competition = competition("stale", CompetitionStatus.Finished);
        Date staleMatch = Date.from(Instant.now().minus(Duration.ofDays(30)));
        givenCompetitionForCollection(competition, Optional.of(staleMatch), 0);

        service.fetchCompetitionContests();

        verify(cyanideApiService, never()).loadContests(competition);
    }

    private void givenCompetitionForCollection(
            Competition competition,
            Optional<Date> lastMatchDate,
            int liveContests) {
        when(cyanideApiProperties.isJobCreationSchedulerActive()).thenReturn(true);
        DataCollection collection = new DataCollection(competition.getId(), EntityType.Competition);
        when(dataCollectionRepository.findByCollectionType(EntityType.Competition))
                .thenReturn(List.of(collection));
        when(competitionRepository.findAllById(anyList())).thenReturn(List.of(competition));
        when(matchDomainService.getLastMatchDatesForCompetitions(List.of(competition.getId())))
                .thenReturn(Map.of(competition.getId(), lastMatchDate));
        if (competition.getStatus() != CompetitionStatus.InProgress) {
            when(contestRepository.countByCompetitionIdAndLive(competition.getId(), 1))
                    .thenReturn(liveContests);
        }
    }

    private Competition competition(String id, CompetitionStatus status) {
        Identity leagueId = new SimpleIdentity("league", 3);
        Competition competition = new Competition(new CompositeIdentity(3, "league", id));
        competition.setLeagueId(leagueId);
        competition.setStatus(status);
        return competition;
    }
}
