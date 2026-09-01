package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.MatchInterpretationRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.domain.stage.ArchiveMatchProvider;
import net.warp_scores.warpscores.domain.stage.Bb1MatchAdapter;
import net.warp_scores.warpscores.domain.stage.Bb2MatchAdapter;
import net.warp_scores.warpscores.domain.stage.Bb3MatchAdapter;
import net.warp_scores.warpscores.domain.stage.MatchAdapterRegistry;
import net.warp_scores.warpscores.domain.stage.StageMatchView;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchInterpretation;
import net.warp_scores.warpscores.model.Platform;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StageMatchServiceTest {
    private StageRepository stageRepository;
    private StageSourceRepository stageSourceRepository;
    private MatchRepository matchRepository;
    private MatchInterpretationRepository interpretationRepository;
    private ArchiveMatchProvider archiveMatchProvider;
    private StageMatchService service;

    @BeforeEach
    void setUp() {
        stageRepository = mock(StageRepository.class);
        stageSourceRepository = mock(StageSourceRepository.class);
        matchRepository = mock(MatchRepository.class);
        interpretationRepository = mock(MatchInterpretationRepository.class);
        archiveMatchProvider = mock(ArchiveMatchProvider.class);
        MatchAdapterRegistry adapters = new MatchAdapterRegistry(List.of(
                new Bb1MatchAdapter(),
                new Bb2MatchAdapter(),
                new Bb3MatchAdapter()));
        service = new StageMatchService(
                stageRepository,
                stageSourceRepository,
                matchRepository,
                interpretationRepository,
                adapters,
                List.of(archiveMatchProvider));
                when(interpretationRepository.findRelevantToMatchIds(anyList(), any(Pattern.class)))
                        .thenReturn(List.of());
    }

    @Test
    void combinesMultiplePhysicalCompetitionsIntoOneStage() {
        String stageId = "nst:s28:main_east";
        StageSource first = competitionSource("source-a", stageId, "competition-a", GameType.BB3);
        StageSource second = competitionSource("source-b", stageId, "competition-b", GameType.BB3);
        Match firstMatch = match("match-a", "competition-a", 3, 1, 0, 1);
        Match secondMatch = match("match-b", "competition-b", 3, 2, 2, 3);
        givenStage(stageId, List.of(first, second));
        when(matchRepository.findByCompetitionId(first.getSourceEntityId())).thenReturn(List.of(firstMatch));
        when(matchRepository.findByCompetitionId(second.getSourceEntityId())).thenReturn(List.of(secondMatch));

        List<StageMatchView> result = service.getMatchesForStage(stageId);

        assertThat(result).extracting(StageMatchView::sourceMatchKey)
                .containsExactly("match-a", "match-b");
        assertThat(result).extracting(StageMatchView::stageSourceId)
                .containsExactly("source-a", "source-b");
        assertThat(result).allMatch(match -> match.capabilities().playerMatchStats());
                verify(interpretationRepository).findRelevantToMatchIds(anyList(), any(Pattern.class));
                verify(interpretationRepository, never()).findAll();
    }

    @Test
    void mergesArchiveMatchesAppliesIdBoundariesAndExcludesInterpretedDisconnect() {
        String stageId = "nst:s11:main";
        StageSource source = leagueSource("source-bb1", stageId, "568");
        source.setIsArchived(true);
        source.setFirstId("1e00201604");
        source.setLastId("1e002016a2");
        Match beforeBoundary = match("1e002015ff", "568", 1, 0, 0, 1);
        Match disconnected = match("1e00201604", "568", 1, 1, 0, 2);
        Match replay = match("1e002016a1", "568", 1, 2, 1, 3);
        Match last = match("1e002016a2", "568", 1, 3, 0, 4);
        givenStage(stageId, List.of(source));
        when(matchRepository.findByLeagueId(source.getSourceEntityId()))
                .thenReturn(List.of(disconnected, last));
        when(archiveMatchProvider.supports(source)).thenReturn(true);
        when(archiveMatchProvider.findMatches(source)).thenReturn(List.of(beforeBoundary, replay));

        MatchInterpretation interpretation = new MatchInterpretation();
        interpretation.setId("bb1:1e00201604");
        interpretation.setSourceMatchId("1e00201604");
        interpretation.setClassification("disconnected");
        interpretation.setReplacementMatchId("1e002016a1");
        interpretation.setExcluded(true);
        when(interpretationRepository.findRelevantToMatchIds(anyList(), any(Pattern.class)))
                .thenReturn(List.of(interpretation));

        assertThat(service.getAllMatchesForStage(stageId))
                .extracting(StageMatchView::sourceMatchKey)
                .containsExactly("1e00201604", "1e002016a1", "1e002016a2");
        assertThat(service.getMatchesForStage(stageId))
                .extracting(StageMatchView::sourceMatchKey)
                .containsExactly("1e002016a1", "1e002016a2");
        assertThat(service.getMatchesForStage(stageId))
                .allMatch(match -> !match.capabilities().playerMatchStats());
    }

    @Test
    void appliesInclusiveIndexBoundariesAfterOrderingMatches() {
        String stageId = "nst:s1:main";
        StageSource source = leagueSource("source-indexed", stageId, "old-league");
        source.setFirstIndex(1);
        source.setLastIndex(2);
        givenStage(stageId, List.of(source));
        when(matchRepository.findByLeagueId(source.getSourceEntityId())).thenReturn(List.of(
                match("third", "old-league", 1, 0, 0, 3),
                match("first", "old-league", 1, 0, 0, 1),
                match("fourth", "old-league", 1, 0, 0, 4),
                match("second", "old-league", 1, 0, 0, 2)));

        assertThat(service.getMatchesForStage(stageId))
                .extracting(StageMatchView::sourceMatchKey)
                .containsExactly("second", "third");
    }

            @Test
            void keepsTheFirstOrderedSourceWhenSourcesContainTheSameMatch() {
                String stageId = "nst:s1:duplicates";
                StageSource first = competitionSource("source-a", stageId, "competition-a", GameType.BB3);
                StageSource second = competitionSource("source-b", stageId, "competition-b", GameType.BB3);
                givenStage(stageId, List.of(second, first));
                when(matchRepository.findByCompetitionId(first.getSourceEntityId()))
                        .thenReturn(List.of(match("same-match", "competition-a", 3, 1, 0, 1)));
                when(matchRepository.findByCompetitionId(second.getSourceEntityId()))
                        .thenReturn(List.of(match("same-match", "competition-b", 3, 0, 1, 2)));

                List<StageMatchView> result = service.getMatchesForStage(stageId);

                assertThat(result).singleElement()
                        .extracting(StageMatchView::stageSourceId)
                        .isEqualTo("source-a");
            }

        @Test
        void returnsNoMatchesForAnEmptyValidSource() {
                String stageId = "nst:s1:empty";
                StageSource source = leagueSource("source-empty", stageId, "empty-league");
                givenStage(stageId, List.of(source));
                when(matchRepository.findByLeagueId(source.getSourceEntityId())).thenReturn(List.of());

                assertThat(service.getMatchesForStage(stageId)).isEmpty();
        }

            @Test
            void rejectsMissingStageAndInvalidBoundaries() {
                assertThatThrownBy(() -> service.getMatchesForStage("missing"))
                        .isInstanceOf(StageNotFoundException.class);

                String stageId = "nst:s1:boundaries";
                StageSource source = leagueSource("source-boundaries", stageId, "league");
                source.setFirstId("missing");
                givenStage(stageId, List.of(source));
                when(matchRepository.findByLeagueId(source.getSourceEntityId()))
                        .thenReturn(List.of(match("first", "league", 1, 0, 0, 1)));
                assertThatThrownBy(() -> service.getMatchesForStage(stageId))
                        .isInstanceOf(IllegalStateException.class);

                source.setFirstId("second");
                source.setLastId("first");
                when(matchRepository.findByLeagueId(source.getSourceEntityId())).thenReturn(List.of(
                        match("first", "league", 1, 0, 0, 1),
                        match("second", "league", 1, 0, 0, 2)));
                assertThatThrownBy(() -> service.getMatchesForStage(stageId))
                        .isInstanceOf(IllegalStateException.class);
            }

            @Test
            void adaptsBb2Matches() {
                String stageId = "nst:s1:bb2";
                StageSource source = competitionSource("source-bb2", stageId, "competition", GameType.BB2);
                givenStage(stageId, List.of(source));
                when(matchRepository.findByCompetitionId(source.getSourceEntityId()))
                        .thenReturn(List.of(match("bb2-match", "competition", 2, 1, 0, 1)));

                assertThat(service.getMatchesForStage(stageId)).singleElement()
                        .extracting(StageMatchView::capabilities)
                        .isEqualTo(new StageMatchView.Capabilities(true, true, false));
            }

            @Test
            void returnsNoMatchesWhenStageHasNoSources() {
                String stageId = "nst:s1:no-sources";
                givenStage(stageId, List.of());

                assertThat(service.getMatchesForStage(stageId)).isEmpty();
            }

    private void givenStage(String stageId, List<StageSource> sources) {
        Stage stage = new Stage();
        stage.setId(stageId);
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(stageSourceRepository.findByStageId(stageId)).thenReturn(sources);
    }

    private StageSource competitionSource(String id, String stageId, String competitionId, GameType game) {
        StageSource source = new StageSource();
        source.setId(id);
        source.setStageId(stageId);
        source.setSourceType(EntityType.Competition);
        source.setSourceEntityId(new SimpleIdentity(competitionId, game == GameType.BB3 ? 3 : 2));
        source.setGame(game);
        source.setPlatform(Platform.PC);
        return source;
    }

    private StageSource leagueSource(String id, String stageId, String leagueId) {
        StageSource source = new StageSource();
        source.setId(id);
        source.setStageId(stageId);
        source.setSourceType(EntityType.League);
        source.setSourceEntityId(new SimpleIdentity(leagueId, 1));
        source.setGame(GameType.BB1);
        source.setPlatform(Platform.PC);
        return source;
    }

    private Match match(
            String matchId,
            String sourceId,
            int opus,
            int homeScore,
            int awayScore,
            long order) {
        Match match = new Match(new SimpleIdentity(matchId, opus));
        match.setMatchId(matchId);
        match.setCompetitionId(new SimpleIdentity(sourceId, opus));
        match.setStarted(Date.from(Instant.ofEpochSecond(order)));
        match.setFinished(Date.from(Instant.ofEpochSecond(order + 1)));
        match.setIsFinalized(true);
        Team home = new Team(new SimpleIdentity(matchId + "-home", opus));
        home.setScore(homeScore);
        Team away = new Team(new SimpleIdentity(matchId + "-away", opus));
        away.setScore(awayScore);
        match.setTeams(new Team[]{home, away});
        return match;
    }
}
