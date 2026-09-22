package net.warp_scores.warpscores.ai.provider.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GeminiInteractionMapper {
    private static final String PROVIDER_ID = "gemini";
    private final ObjectMapper objectMapper;

    public ObjectNode requestBody(CanonicalLlmRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", request.model());
        body.put("store", false);
        body.put("stream", false);
        body.put("system_instruction", String.join("\n", request.context().hardConstraints()));

        try {
            body.put("input",
                    request.taskInstruction()
                            + "\n\nCANONICAL_CONTEXT_JSON:\n"
                            + objectMapper.writeValueAsString(request.context()));
        } catch (JsonProcessingException e) {
            throw new LlmProviderException(
                    PROVIDER_ID,
                    LlmProviderException.Kind.MALFORMED_RESPONSE,
                    null,
                    "Could not serialize canonical Gemini context",
                    e);
        }

        ObjectNode generationConfig = body.putObject("generation_config");
        if (request.options().temperature() != null) {
            generationConfig.put("temperature", request.options().temperature());
        }
        if (request.options().maxOutputTokens() != null) {
            generationConfig.put("max_output_tokens", request.options().maxOutputTokens());
        }
        if (generationConfig.isEmpty()) {
            body.remove("generation_config");
        }

        if (request.outputContract().format() == OutputContract.Format.JSON) {
            ObjectNode format = body.putObject("response_format");
            format.put("type", "text");
            format.put("mime_type", "application/json");
            try {
                format.set("schema", objectMapper.readTree(request.outputContract().schemaJson()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON response schema", e);
            }
        }

        return body;
    }

    public CanonicalLlmResponse response(String requestedModel, JsonNode root) {
        String status = text(root, "status");
        if ("failed".equals(status) || "cancelled".equals(status)) {
            throw new LlmProviderException(
                    PROVIDER_ID,
                    LlmProviderException.Kind.REJECTED,
                    null,
                    "Gemini interaction ended with status " + status + errorSuffix(root));
        }
        if ("requires_action".equals(status) || "in_progress".equals(status)) {
            throw new LlmProviderException(
                    PROVIDER_ID,
                    LlmProviderException.Kind.MALFORMED_RESPONSE,
                    null,
                    "Unexpected synchronous Gemini interaction status: " + status);
        }

        List<String> parts = new ArrayList<>();
        JsonNode steps = root.path("steps");
        if (steps.isArray()) {
            for (JsonNode step : steps) {
                if (!"model_output".equals(text(step, "type"))) continue;
                JsonNode content = step.path("content");
                if (!content.isArray()) continue;
                for (JsonNode item : content) {
                    if ("text".equals(text(item, "type")) && item.hasNonNull("text")) {
                        parts.add(item.get("text").asText());
                    }
                }
            }
        }
        if (parts.isEmpty()) {
            throw new LlmProviderException(
                    PROVIDER_ID,
                    LlmProviderException.Kind.MALFORMED_RESPONSE,
                    null,
                    "Gemini response contained no model text" + errorSuffix(root));
        }

        JsonNode usage = root.path("usage");
        Integer input = integer(usage, "total_input_tokens");
        Integer output = integer(usage, "total_output_tokens");
        String actualModel = text(root, "model");
        if (actualModel == null || actualModel.isBlank()) actualModel = requestedModel;

        return new CanonicalLlmResponse(
                PROVIDER_ID,
                actualModel,
                text(root, "id"),
                String.join("", parts),
                new CanonicalLlmResponse.Usage(input, output),
                status);
    }

    private static String errorSuffix(JsonNode root) {
        JsonNode errors = root.path("errors");
        if (!errors.isArray() || errors.isEmpty()) return "";
        JsonNode first = errors.get(0);
        String message = text(first, "message");
        return message == null ? "" : ": " + message;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isNumber() ? null : value.asInt();
    }
}
