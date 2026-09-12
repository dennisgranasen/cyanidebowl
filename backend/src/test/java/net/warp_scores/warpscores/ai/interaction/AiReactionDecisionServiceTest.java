package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.CommunityReaction;
import net.warp_scores.warpscores.model.MatchArticle;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiReactionDecisionServiceTest {
    private final ContextAssemblyService contextAssembly =
            mock(ContextAssemblyService.class);
    private final LlmExecutionService llm = mock(LlmExecutionService.class);
    private final AiReactionDecisionService service = new AiReactionDecisionService(
            new ObjectMapper(),
            new ContextPlanner(),
            contextAssembly,
            llm);

    @Test
    void choosesStrongReactionFromReporterDecisionInsteadOfRandomly() {
        when(contextAssembly.assemble(any())).thenReturn(context());
        when(llm.generate(any(), any())).thenReturn(response(
                "{\"reaction\":\"TRIPLE_SKULL\"}"));

        CommunityReaction.Type result =
                service.chooseForArticle(reporter(), article());

        assertThat(result).isEqualTo(CommunityReaction.Type.TRIPLE_SKULL);

        ArgumentCaptor<CanonicalLlmRequest> request =
                ArgumentCaptor.forClass(CanonicalLlmRequest.class);
        verify(llm).generate(eq("putridia"), request.capture());
        assertThat(request.getValue().taskType())
                .isEqualTo(ContextTaskType.ARTICLE_COMMENT);
        assertThat(request.getValue().taskInstruction())
                .contains("not a random roll");
    }

    @Test
    void commentReactionUsesSocialReplyContext() {
        when(contextAssembly.assemble(any())).thenReturn(context());
        when(llm.generate(any(), any())).thenReturn(response(
                "{\"reaction\":\"DOUBLE_POW\"}"));

        CommunityComment comment = new CommunityComment();
        comment.setBody("I agree completely.");
        comment.setAuthorDisplayName("Coach");

        CommunityReaction.Type result =
                service.chooseForComment(reporter(), article(), comment);

        assertThat(result).isEqualTo(CommunityReaction.Type.DOUBLE_POW);

        ArgumentCaptor<CanonicalLlmRequest> request =
                ArgumentCaptor.forClass(CanonicalLlmRequest.class);
        verify(llm).generate(eq("putridia"), request.capture());
        assertThat(request.getValue().taskType())
                .isEqualTo(ContextTaskType.SOCIAL_REPLY);
    }

    @Test
    void rejectsReactionOutsideCanonicalReactionEnum() {
        when(contextAssembly.assemble(any())).thenReturn(context());
        when(llm.generate(any(), any())).thenReturn(response(
                "{\"reaction\":\"LIKE\"}"));

        assertThatThrownBy(() -> service.chooseForArticle(reporter(), article()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported reaction");
    }

    private static AiReporterDefinition reporter() {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId("putridia");
        reporter.setAlias("Lady Putridia");
        reporter.setUserId(42L);
        return reporter;
    }

    private static MatchArticle article() {
        MatchArticle article = new MatchArticle();
        article.setId("article-1");
        article.setMatchId("match-1");
        article.setTitle("A brutal evening");
        article.setBody("The home side won.");
        return article;
    }

    private static AssembledContext context() {
        return new AssembledContext(
                "world-v1", List.of(), Map.of(), 0, 0);
    }

    private static CanonicalLlmResponse response(String content) {
        return new CanonicalLlmResponse(
                "provider", "model", "request", content,
                CanonicalLlmResponse.Usage.unknown(), "completed");
    }
}
