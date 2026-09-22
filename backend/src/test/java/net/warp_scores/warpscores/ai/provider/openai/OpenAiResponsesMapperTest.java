package net.warp_scores.warpscores.ai.provider.openai;

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

class OpenAiResponsesMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiResponsesMapper mapper = new OpenAiResponsesMapper(objectMapper);

    @Test
    void mapsCanonicalRequestToResponsesApi() {
        var json = mapper.requestBody("grok", request(OutputContract.text()), true);

        assertThat(json.path("model").asText()).isEqualTo("grok-test");
        assertThat(json.path("store").asBoolean()).isFalse();
        assertThat(json.path("instructions").asText()).contains("Blood Bowl is real");
        assertThat(json.path("input").asText()).contains("CANONICAL_CONTEXT_JSON");
        assertThat(json.path("max_output_tokens").asInt()).isEqualTo(500);
    }

    @Test
    void mapsStructuredOutputUsingResponsesTextFormat() {
        var json = mapper.requestBody("grok", request(
                new OutputContract(OutputContract.Format.JSON,
                        "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"}}}")), true);

        assertThat(json.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(json.path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(json.path("text").path("format").path("schema").path("type").asText())
                .isEqualTo("object");
    }

    @Test
    void normalizesStandardResponsesOutput() throws Exception {
        var root = objectMapper.readTree("""
                {
                  "id":"resp-123",
                  "model":"grok-4.6",
                  "status":"completed",
                  "output":[
                    {"type":"message","content":[{"type":"output_text","text":"hello"}]}
                  ],
                  "usage":{"input_tokens":12,"output_tokens":7}
                }
                """);

        var result = mapper.response("grok", "requested", root);

        assertThat(result.providerId()).isEqualTo("grok");
        assertThat(result.providerRequestId()).isEqualTo("resp-123");
        assertThat(result.model()).isEqualTo("grok-4.6");
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.usage().inputTokens()).isEqualTo(12);
        assertThat(result.usage().outputTokens()).isEqualTo(7);
    }

    private static CanonicalLlmRequest request(OutputContract contract) {
        return new CanonicalLlmRequest(
                "lady-putridia",
                "1",
                ContextTaskType.EDITORIAL_ARTICLE,
                "grok-test",
                new AssembledContext(
                        "world-v1",
                        List.of("Blood Bowl is real"),
                        Map.of(ContextSection.DOMAIN, List.of()),
                        20,
                        0),
                "Write an article.",
                contract,
                new GenerationOptions(1.0, 500));
    }
}
