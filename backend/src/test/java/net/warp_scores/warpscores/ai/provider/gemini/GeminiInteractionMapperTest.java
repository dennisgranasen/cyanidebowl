package net.warp_scores.warpscores.ai.provider.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextSection;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiInteractionMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiInteractionMapper mapper = new GeminiInteractionMapper(objectMapper);

    @Test
    void mapsCanonicalRequestToStatelessInteractionsApi() {
        CanonicalLlmRequest request = request(OutputContract.text());

        var json = mapper.requestBody(request);

        assertThat(json.path("model").asText()).isEqualTo("gemini-test");
        assertThat(json.path("store").asBoolean()).isFalse();
        assertThat(json.path("stream").asBoolean()).isFalse();
        assertThat(json.path("system_instruction").asText()).contains("Blood Bowl is real");
        assertThat(json.path("input").asText()).contains("CANONICAL_CONTEXT_JSON");
        assertThat(json.path("generation_config").path("temperature").asDouble()).isEqualTo(1.0);
        assertThat(json.path("generation_config").path("max_output_tokens").asInt()).isEqualTo(500);
    }

    @Test
    void mapsStructuredOutputToCurrentResponseFormatShape() {
        var json = mapper.requestBody(request(
                new OutputContract(OutputContract.Format.JSON,
                        "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"}}}")));

        assertThat(json.path("response_format").path("type").asText()).isEqualTo("text");
        assertThat(json.path("response_format").path("mime_type").asText()).isEqualTo("application/json");
        assertThat(json.path("response_format").path("schema").path("type").asText()).isEqualTo("object");
    }

    @Test
    void normalizesInteractionResponseAndUsage() throws Exception {
        var root = objectMapper.readTree("""
                {
                  "id":"interaction-123",
                  "model":"gemini-3.8-flash",
                  "status":"completed",
                  "steps":[
                    {"type":"model_output","content":[{"type":"text","text":"hello"}]}
                  ],
                  "usage":{"total_input_tokens":12,"total_output_tokens":7}
                }
                """);

        var result = mapper.response("requested-model", root);

        assertThat(result.providerId()).isEqualTo("gemini");
        assertThat(result.providerRequestId()).isEqualTo("interaction-123");
        assertThat(result.model()).isEqualTo("gemini-3.8-flash");
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.usage().inputTokens()).isEqualTo(12);
        assertThat(result.usage().outputTokens()).isEqualTo(7);
        assertThat(result.finishReason()).isEqualTo("completed");
    }

    private static CanonicalLlmRequest request(OutputContract contract) {
        AssembledContext context = new AssembledContext(
                "world-v1",
                List.of("Blood Bowl is real"),
                Map.of(ContextSection.DOMAIN, List.of()),
                20,
                0);
        return new CanonicalLlmRequest(
                "lady-putridia",
                "1",
                ContextTaskType.EDITORIAL_ARTICLE,
                "gemini-test",
                context,
                "Write an article.",
                contract,
                new GenerationOptions(1.0, 500));
    }
}
