package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalContextMapperTest {
    private final CanonicalContextMapper mapper = new CanonicalContextMapper();

    @Test
    void mapsArticleAsAttributedDiscourseWithCanonicalSubjects() {
        Article article = new Article();
        article.setId("a-1");
        article.setAuthorUserId(42L);
        article.setAuthorDisplayName("Lady Putridia");
        article.setLeagueSystemId("nuffle-spitfire");
        article.setTags(List.of("Playoffs", "Undead"));
        article.setTitle("Finalen närmar sig");
        article.setBodyHtml("<p>En <strong>het</strong> kväll &amp; stor publik.</p>");
        article.setPublishedAt(Instant.parse("2026-09-12T06:00:00Z"));

        ContextItem item = mapper.article(article, ContextSource.SELF);

        assertEquals(ContextAuthority.ATTRIBUTED_DISCOURSE, item.authority());
        assertEquals(42L, item.authorUserId());
        assertEquals("En het kväll & stor publik.", item.body());
        assertTrue(item.subjects().contains(new SubjectRef(SubjectType.ARTICLE, "a-1")));
        assertTrue(item.subjects().contains(new SubjectRef(SubjectType.LEAGUE_SYSTEM, "nuffle-spitfire")));
        assertTrue(item.subjects().contains(SubjectRef.topic("PLAYOFFS")));
    }

    @Test
    void commentInheritsArticleSubjectsButRemainsDiscourse() {
        CommunityComment comment = new CommunityComment();
        comment.setId("c-1");
        comment.setTargetType(CommunityComment.TargetType.ARTICLE);
        comment.setTargetId("a-1");
        comment.setAuthorUserId(7L);
        comment.setBody("Domaren såg ingenting.");

        ContextItem item = mapper.comment(comment, ContextSource.OTHER_USERS,
                List.of(SubjectRef.topic("final")));

        assertEquals(ContextAuthority.ATTRIBUTED_DISCOURSE, item.authority());
        assertEquals(new SubjectRef(SubjectType.ARTICLE, "a-1"), item.thread());
        assertTrue(item.subjects().contains(SubjectRef.topic("final")));
    }

    @Test
    void mapsMatchOnlyAsDomainFactAndUsesStableMongoIds() {
        Match match = new Match(new SimpleIdentity("match-doc", 3));
        match.setMatchId("game-123");
        match.setCompetitionId(new SimpleIdentity("comp-7", 3));
        Team home = new Team(new SimpleIdentity("team-a", 3));
        home.setName("Råttfällan");
        Team away = new Team(new SimpleIdentity("team-b", 3));
        away.setName("Nottingham");
        match.setTeams(new Team[]{home, away});
        Match.Coach coach = new Match.Coach();
        coach.setId("coach-17");
        match.setCoaches(new Match.Coach[]{coach});

        ContextItem item = mapper.match(match, ContextSource.DOMAIN, List.of());

        assertEquals(ContextAuthority.DOMAIN_FACT, item.authority());
        assertNull(item.authorUserId());
        assertTrue(item.subjects().contains(new SubjectRef(SubjectType.MATCH, "game-123")));
        assertTrue(item.subjects().contains(new SubjectRef(SubjectType.COMPETITION, "3_comp-7")));
        assertTrue(item.subjects().contains(new SubjectRef(SubjectType.TEAM, "3_team-a")));
        assertTrue(item.subjects().contains(new SubjectRef(SubjectType.COACH_IDENTITY, "coach-17")));
    }
}
