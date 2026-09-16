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
        var service = new CommunityDirectoryService(profiles, seasons, scopes, mongo);
        var member = new AiCommunityMemberProfile(); member.setId("fan"); member.setUserId(7L); member.setTeamId("team"); member.setActive(false);
        var outside = new AiCommunityMemberProfile(); outside.setId("other"); outside.setTeamId("outside");
        when(profiles.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(member, outside));
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
        var directory = service.directory("league");
        assertEquals(1, directory.members().size());
        assertFalse(directory.members().getFirst().profile().isActive());
        assertEquals(1, directory.members().getFirst().commentCount());
        assertEquals(List.of("season"), directory.members().getFirst().seasonIds());
        var history = service.history("fan", 0);
        assertEquals(1, history.total());
        assertEquals("/community/discussion/ARTICLE/public#comment-one", history.comments().getFirst().url());
        assertTrue(service.history("fan", 1).comments().isEmpty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.discussion(CommunityComment.TargetType.ARTICLE, "draft"));
    }
    private CommunityComment comment(String id, String target) {
        var c = new CommunityComment(); c.setId(id); c.setAuthorUserId(7L); c.setTargetId(target); c.setTargetType(CommunityComment.TargetType.ARTICLE); c.setBody("Comment"); return c;
    }
}
