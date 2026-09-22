package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.provider.*;
import net.warp_scores.warpscores.ai.scheduling.AiAutonomousWorkQueue;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import net.warp_scores.warpscores.service.ArticleImageSubjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaggedImageFanInteractionTest {
    @Mock AiCommunityMemberProfileRepository profiles;
    @Mock ArticleRepository articles;
    @Mock MatchArticleRepository matchArticles;
    @Mock MatchRepository matches;
    @Mock CommunityCommentRepository comments;
    @Mock AiInitiativePolicyService initiative;
    @Mock ContextPlanner planner;
    @Mock ContextAssemblyService assembly;
    @Mock LlmExecutionService llm;
    @Mock ArticleImageSubjects images;
    @Mock AiAutonomousWorkQueue queue;
    @InjectMocks AiCommunityFanInteractionService service;

    @Test void taggedFanIsQueuedRegardlessOfProbabilityAndTeamAffinity() {
        var article = article();
        when(images.images("image html")).thenReturn(List.of(image("fan")));
        service.onArticlePublished(article);
        var job = ArgumentCaptor.forClass(AiAutonomousWorkQueue.EnqueueRequest.class);
        verify(queue).enqueue(job.capture());
        assertEquals("fan", job.getValue().actorId());
        assertEquals(5, job.getValue().maxAttempts());
        verifyNoInteractions(initiative);
    }
    @Test void taggedFanCommentsEvenWithoutTeamAffinityAndDoesNotDuplicate() {
        var article = article();
        var fan = new AiCommunityMemberProfile(); fan.setId("fan"); fan.setUserId(5L); fan.setUserSubject("ai:fan"); fan.setTeamId("unrelated"); fan.setDisplayName("Grub");
        when(profiles.findById("fan")).thenReturn(Optional.of(fan));
        when(articles.findById("article")).thenReturn(Optional.of(article));
        when(images.images("image html")).thenReturn(List.of(image("fan")));
        when(images.descriptions("image html")).thenReturn("Grub in a fountain");
        when(assembly.assemble(any())).thenReturn(new AssembledContext("world", List.of(), Map.of(), 0, 0));
        when(llm.generate(eq("fan"), any())).thenReturn(new CanonicalLlmResponse("test", "model", null, "That's me!", null, "stop"));
        service.commentOnTaggedImageOnce("ARTICLE", "article", "fan", "image");
        var saved = ArgumentCaptor.forClass(CommunityComment.class); verify(comments).save(saved.capture());
        assertEquals("That's me!", saved.getValue().getBody());
        verify(llm).generate(eq("fan"), argThat(r -> r.taskInstruction().contains("Grub in a fountain")));
        when(comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(CommunityComment.TargetType.ARTICLE, "article")).thenReturn(List.of(saved.getValue()));
        service.commentOnTaggedImageOnce("ARTICLE", "article", "fan", "image");
        verify(llm, times(1)).generate(anyString(), any());
        var another = new ArticleImageSubjects.Image("second-image", "/second.png", image("fan").associations(), "Another scene");
        when(images.images("image html")).thenReturn(List.of(image("fan"), another));
        service.commentOnTaggedImageOnce("ARTICLE", "article", "fan", "second-image");
        verify(llm, times(2)).generate(anyString(), any());
        verify(comments, times(2)).save(any());
        verifyNoInteractions(initiative);
    }
    @Test void taggedMatchFanIsQueuedEvenWhenTheirTeamDidNotPlay() {
        var article = new MatchArticle(); article.setId("match-article"); article.setMatchId("match");
        article.setStatus(MatchArticle.Status.PUBLISHED); article.setBodyHtml("image html");
        when(images.images("image html")).thenReturn(List.of(image("visitor")));
        when(matches.findFirstByMatchId("match")).thenReturn(Optional.of(new Match(new net.warp_scores.warpscores.identity.SimpleIdentity("match", 3))));
        service.onMatchArticlePublished(article);
        verify(queue).enqueue(argThat(job -> "visitor".equals(job.actorId()) && "MATCH_ARTICLE".equals(job.targetType())));
        verifyNoInteractions(initiative);
    }
    @Test void removedImageDoesNotCommentWhenQueuedWorkRuns() {
        var fan = new AiCommunityMemberProfile(); fan.setUserId(5L); fan.setUserSubject("ai:fan");
        when(profiles.findById("fan")).thenReturn(Optional.of(fan));
        when(articles.findById("article")).thenReturn(Optional.of(article()));
        service.commentOnTaggedImageOnce("ARTICLE", "article", "fan", "image");
        verifyNoInteractions(llm, comments, initiative);
    }
    private ArticleImageSubjects.Image image(String fanId) {
        return new ArticleImageSubjects.Image("image", "/image.png", List.of(new Article.Association(Article.LinkType.FAN, fanId)), "Grub in a fountain");
    }
    private Article article() { var a = new Article(); a.setId("article"); a.setStatus(Article.Status.PUBLISHED); a.setBodyHtml("image html"); return a; }
}
