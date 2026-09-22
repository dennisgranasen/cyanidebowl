package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.model.Article;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static net.warp_scores.warpscores.model.Article.LinkType.*;

class ArticleScopeServiceTest {
    @Test void historicalParticipationIsIncludedInAudience() {
        var mongo = org.mockito.Mockito.mock(org.springframework.data.mongodb.core.MongoTemplate.class);
        var seasons = org.mockito.Mockito.mock(net.warp_scores.warpscores.domain.persistence.SeasonRepository.class);
        var stages = org.mockito.Mockito.mock(net.warp_scores.warpscores.domain.persistence.StageRepository.class);
        var matches = org.mockito.Mockito.mock(StageMatchService.class);
        var participation = org.mockito.Mockito.mock(net.warp_scores.warpscores.domain.persistence.MatchPlayerParticipationRepository.class);
        var permissions = org.mockito.Mockito.mock(UserPermissionService.class);
        var service = new ArticleScopeService(mongo, seasons, stages, matches, participation, permissions);
        var season = new net.warp_scores.warpscores.model.Season(); season.setId("s4"); season.setLeagueSystemId("league");
        var stage = new net.warp_scores.warpscores.model.Stage(); stage.setId("stage");
        var match = org.mockito.Mockito.mock(net.warp_scores.warpscores.domain.stage.StageMatchView.class);
        var oldPlayer = new net.warp_scores.warpscores.model.MatchPlayerParticipation(); oldPlayer.setPlayerId("retired-player");
        org.mockito.Mockito.when(seasons.findById("s4")).thenReturn(java.util.Optional.of(season));
        org.mockito.Mockito.when(stages.findBySeasonIdOrderBySequenceAsc("s4")).thenReturn(List.of(stage));
        org.mockito.Mockito.when(matches.getMatchesForStage("stage")).thenReturn(List.of(match));
        org.mockito.Mockito.when(match.sourceMatchKey()).thenReturn("historical-match");
        org.mockito.Mockito.when(participation.findByMatchIdIn(List.of("historical-match"))).thenReturn(List.of(oldPlayer));
        assertTrue(service.audience("league", "s4").playerIds().contains("retired-player"));
    }

    @Test void feedLimitIsAppliedAfterSubjectRelevance() {
        var mongo = org.mockito.Mockito.mock(org.springframework.data.mongodb.core.MongoTemplate.class);
        var service = new ArticleScopeService(mongo, null, null, null, null, null);
        Article irrelevant = article(link(TEAM, "another-team"));
        Article expected = article(link(TEAM, "requested-team"));
        var stream = java.util.stream.Stream.concat(java.util.stream.Stream.generate(() -> irrelevant).limit(150), java.util.stream.Stream.of(expected));
        org.mockito.Mockito.when(mongo.stream(org.mockito.ArgumentMatchers.any(org.springframework.data.mongodb.core.query.Query.class), org.mockito.ArgumentMatchers.eq(Article.class))).thenReturn(stream);
        assertEquals(List.of(expected), service.feed(null, null, TEAM, "requested-team", 1));
    }

    private final ArticleScopeService.Audience season = new ArticleScopeService.Audience("league", Set.of("s4"), Set.of("old-team", "current-team"), Set.of("old-player"));
    private Article article(Article.Association... links) {
        Article article = new Article(); article.setAssociations(List.of(links)); return article;
    }
    private Article.Association link(Article.LinkType type, String id) { return new Article.Association(type, id); }
    @Test void globalAndFanOnlyArticlesReachAllSeasons() {
        assertTrue(ArticleScopeService.relevant(article(), season));
        assertTrue(ArticleScopeService.relevant(article(link(FAN, "fan")), season));
        assertTrue(ArticleScopeService.relevant(article(link(STAFF, "writer")), season));
    }
    @Test void generalLeagueNewsDoesNotLeakIntoAnotherLeague() {
        assertTrue(ArticleScopeService.relevant(article(link(LEAGUE_SYSTEM, "league")), season));
        assertFalse(ArticleScopeService.relevant(article(link(LEAGUE_SYSTEM, "other")), season));
    }
    @Test void explicitSeasonRestrictsEvenMatchingLeagueTeamAndPlayer() {
        assertFalse(ArticleScopeService.relevant(article(link(LEAGUE_SYSTEM, "league"), link(SEASON, "s3"), link(TEAM, "old-team"), link(PLAYER, "old-player")), season));
        assertTrue(ArticleScopeService.relevant(article(link(SEASON, "s3"), link(SEASON, "s4")), season));
    }
    @Test void pastAndCurrentParticipantsAreRelevantWithoutExplicitSeason() {
        assertTrue(ArticleScopeService.relevant(article(link(TEAM, "old-team")), season));
        assertTrue(ArticleScopeService.relevant(article(link(TEAM, "current-team")), season));
        assertTrue(ArticleScopeService.relevant(article(link(PLAYER, "old-player")), season));
        assertFalse(ArticleScopeService.relevant(article(link(LEAGUE_SYSTEM, "league"), link(TEAM, "unrelated")), season));
    }
    @Test void legacyScopeHasTheSamePrecedence() {
        Article article = new Article(); article.setLeagueSystemId("league"); article.setSeasonId("s3");
        article.setTeamIds(List.of("old-team"));
        assertFalse(ArticleScopeService.relevant(article, season));
        article.setSeasonId(null);
        assertTrue(ArticleScopeService.relevant(article, season));
    }
    @Test void teamOrPlayerOnlyArticlesAreNotGlobal() {
        assertFalse(ArticleScopeService.global(List.of(link(TEAM, "team"))));
        assertFalse(ArticleScopeService.global(List.of(link(PLAYER, "player"))));
        assertFalse(ArticleScopeService.global(List.of(link(SEASON, "season"))));
    }
}
