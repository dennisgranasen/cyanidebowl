package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleGenerationLlmRequestFactoryTest {
    private final ArticleGenerationLlmRequestFactory factory =
            new ArticleGenerationLlmRequestFactory();

    @Test
    void defaultInstructionKeepsReplayBookkeepingAsContextButForbidsPublishingIt() {
        AssembledContext context = new AssembledContext(
                "world-v1",
                List.of(),
                Map.of(),
                0,
                0
        );

        CanonicalLlmRequest request = factory.create(
                "reporter-1",
                "1",
                "test-model",
                context,
                null,
                null
        );

        assertThat(request.context()).isSameAs(context);
        String instruction = request.taskInstruction().replaceAll("\\s+", " ");

        assertThat(instruction)
                .contains("global turn number")
                .contains("team_turn")
                .contains("drive")
                .contains("half")
                .contains("Never expose those counters as numbered turns")
                .contains("turn 27")
                .contains("the 27th round")
                .contains("natural in-world timing")
                .contains("Keep the raw context intact");
    }

    @Test
    void replayNarrativePolicyAlsoAppliesWhenAnEditorialBriefIsPresent() {
        AssembledContext context = new AssembledContext(
                "world-v1",
                List.of(),
                Map.of(),
                0,
                0
        );

        CanonicalLlmRequest request = factory.create(
                "reporter-1",
                "1",
                "test-model",
                context,
                "Write this as an angry post-match column.",
                null
        );
        String instruction = request.taskInstruction().replaceAll("\\s+", " ");
        assertThat(instruction)
                .contains("Never expose those counters as numbered turns")
                .contains("Editorial brief:")
                .contains("Write this as an angry post-match column.");
    }
}
