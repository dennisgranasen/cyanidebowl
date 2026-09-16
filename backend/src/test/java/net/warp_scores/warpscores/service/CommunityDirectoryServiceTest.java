package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CommunityDirectoryServiceTest {
    @Test void directoryAndHistoryCountOnlyVisibleCommentsAndPreserveInactiveMembers() {
        var profiles = mock(AiCommunityMemberProfileRepository.class);
        var seasons = mock(SeasonRepository.class);
        var scopes = mock(ArticleScopeService.class);
        var mongo = mock(MongoTemplate.class);
        var service = new CommunityDirectoryService(profiles, seasons, mongo);
        var member = new AiCommunityMemberProfile(); member.setId("fan"); member.setUserId(7L); member.setTeamId("3_team"); member.setActive(false);
        var outside = new AiCommunityMemberProfile(); outside.setId("other"); outside.setTeamId("outside");
        when(mongo.find(any(Query.class), eq(AiCommunityMemberProfile.class))).thenReturn(List.of(member));
        when(profiles.findById("fan")).thenReturn(Optional.of(member));
        var season = new Season(); season.setId("season"); season.setLeagueSystemId("league");
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("league")).thenReturn(List.of(season));
        when(scopes.audience("league", "season")).thenReturn(new ArticleScopeService.Audience("league", Set.of("season"), Set.of("team"), Set.of()));
        var published = new Article(); published.setStatus(Article.Status.PUBLISHED); published.setTitle("News"); published.setSlug("news");
        var draft = new Article(); draft.setStatus(Article.Status.DRAFT);
        when(mongo.findById("public", Article.class)).thenReturn(published);
        when(mongo.findById("draft", Article.class)).thenReturn(draft);
        var visible = comment("one", "public"); var hidden = comment("two", "draft"); var deleted = comment("three", "public"); deleted.setDeletedAt(java.time.Instant.now());
        when(mongo.find(any(Query.class), eq(CommunityComment.class))).thenReturn(List.of(visible, hidden, deleted));
        var source = new StageSource(); source.setSeasonId("season"); source.setSourceType(EntityType.League);
        source.setSourceEntityId(new net.warp_scores.warpscores.identity.SimpleIdentity("league", 3));
        var team = new Team(new net.warp_scores.warpscores.identity.SimpleIdentity("team", 3));
        team.setLeagueIds(new net.warp_scores.warpscores.identity.Identity[]{source.getSourceEntityId()});
        when(mongo.find(any(Query.class), eq(StageSource.class))).thenReturn(List.of(source));
        when(mongo.find(any(Query.class), eq(Team.class))).thenReturn(List.of(team));
        var directory = service.directory("league", null);
        assertEquals(1, directory.members().size());
        assertFalse(directory.members().getFirst().active());
        assertEquals(1, directory.members().getFirst().commentCount());
        assertEquals(List.of("season"), directory.members().getFirst().seasonIds());
        var history = service.history("fan", 0);
        assertEquals(1, history.total());
        assertEquals("/community/discussion/ARTICLE/public#comment-one", history.comments().getFirst().url());
        assertTrue(service.history("fan", 1).comments().isEmpty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.discussion(CommunityComment.TargetType.ARTICLE, "draft"));
    }
    @Test void coachFilterCombinesWithOtherFiltersBeforePaginationAndKeepsAllCoachOptions() {
        var mongo = mock(MongoTemplate.class);
        var seasons = mock(SeasonRepository.class);
        var service = new CommunityDirectoryService(mock(AiCommunityMemberProfileRepository.class), seasons, mongo);
        var season = new Season(); season.setId("s1");
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("league")).thenReturn(List.of(season));
        var source = new StageSource(); source.setSeasonId("s1"); source.setSourceType(EntityType.League);
        source.setSourceEntityId(new net.warp_scores.warpscores.identity.SimpleIdentity("league", 3));
        when(mongo.find(any(Query.class), eq(StageSource.class))).thenReturn(List.of(source));
        var teams = new ArrayList<Team>();
        var people = new ArrayList<AiCommunityMemberProfile>();
        for (int i = 0; i < 4; i++) {
            var team = new Team(new net.warp_scores.warpscores.identity.SimpleIdentity("team" + i, 3));
            team.setLeagueIds(new net.warp_scores.warpscores.identity.Identity[]{source.getSourceEntityId()});
            if (i < 3) {
                team.setCoachId(new net.warp_scores.warpscores.identity.SimpleIdentity(i < 2 ? "coachA" : "coachB", 3));
                team.setCoachName(i < 2 ? "Anna" : "Bertil");
            }
            teams.add(team);
            var person = new AiCommunityMemberProfile(); person.setId("fan" + i);
            person.setTeamId(team.getId().asMongoKey()); person.setDisplayName("Fan " + i);
            people.add(person);
        }
        when(mongo.find(any(Query.class), eq(Team.class))).thenReturn(teams);
        when(mongo.find(any(Query.class), eq(AiCommunityMemberProfile.class))).thenReturn(people);
        var query = new CommunityDirectoryService.DirectoryQuery("", List.of("3_coacha"), "s1", "", "", "active", "name", "asc", "sv", 0, 1);
        var result = service.directory("league", query);
        assertEquals(2, result.filteredTotal());
        assertEquals(4, result.total());
        assertEquals("fan0", result.members().getFirst().id());
        assertTrue(result.hasMore());
        assertEquals(List.of("Anna", "Bertil"), result.facets().coaches().stream().map(CommunityDirectoryService.CoachOption::name).toList());
        result = service.directory("league", new CommunityDirectoryService.DirectoryQuery("3_team2", List.of("3_coacha"), "", "", "", "", "name", "asc", "sv", 0, 24));
        assertTrue(result.members().isEmpty());
        assertEquals(4, service.directory("league", null).members().size());
        result = service.directory("league", new CommunityDirectoryService.DirectoryQuery("", List.of("3_coacha", "3_coachb"), "", "", "", "active", "name", "asc", "sv", 0, 24));
        assertEquals(3, result.filteredTotal());
        assertEquals(List.of("fan0", "fan1", "fan2"), result.members().stream().map(CommunityDirectoryService.Member::id).toList());
    }

    private CommunityComment comment(String id, String target) {
        var c = new CommunityComment(); c.setId(id); c.setAuthorUserId(7L); c.setTargetId(target); c.setTargetType(CommunityComment.TargetType.ARTICLE); c.setBody("Comment"); return c;
    }
}
