package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.service.MatchArticleService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiMatchArticleWorkHandlerTest {
    @Test
    void delegatesMatchAndReporterToInternalGenerationPath() {
        MatchArticleService articles = mock(MatchArticleService.class);
        AiMatchArticleWorkHandler handler = new AiMatchArticleWorkHandler(articles);
        AiAutonomousWorkItem item = new AiAutonomousWorkItem();
        item.setKind(AiAutonomousWorkItem.WorkKind.MATCH_ARTICLE);
        item.setTargetType("MATCH");
        item.setTargetId("match-1");
        item.setActorId("reporter-1");
        item.setPayload(java.util.Map.of("generationSlot", "2"));

        assertThat(handler.handlerKey()).isEqualTo(AiCompletedMatchArticleWorkProducer.HANDLER_KEY);
        handler.execute(item);

        verify(articles).requestAiAutonomously("match-1", "reporter-1", 2);
    }

    @Test
    void rejectsWorkForAnotherKind() {
        AiMatchArticleWorkHandler handler = new AiMatchArticleWorkHandler(mock(MatchArticleService.class));
        AiAutonomousWorkItem item = new AiAutonomousWorkItem();
        item.setKind(AiAutonomousWorkItem.WorkKind.MATCH_COMMENT);
        item.setTargetId("match-1");
        item.setActorId("reporter-1");

        assertThatThrownBy(() -> handler.execute(item))
                .isInstanceOf(IllegalArgumentException.class);
    }
}