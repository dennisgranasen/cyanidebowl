package net.warp_scores.warpscores.ai.provider.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import net.warp_scores.warpscores.ai.provider.OutputContract;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class OpenAiResponsesMapper {
    private final ObjectMapper objectMapper;

    public ObjectNode requestBody(
            String providerId,
            CanonicalLlmRequest request,
            boolean structuredOutputSupported) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", request.model());
        body.put("store", false);
        body.put("instructions", String.join("\n", request.context().hardConstraints()));

        try {
            body.put("input",
                    request.taskInstruction()
                            + "\n\nCANONICAL_CONTEXT_JSON:\n"
                            + objectMapper.writeValueAsString(request.context()));
        } catch (JsonProcessingException e) {
            throw new LlmProviderException(
                    providerId,
                    LlmProviderException.Kind.MALFORMED_RESPONSE,
                    null,
                    "Could not serialize canonical OpenAI-compatible context",
                    e);
        }

        if (request.options().temperature() != null) {
            body.put("temperature", request.options().temperature());
        }
        if (request.options().maxOutputTokens() != null) {
            body.put("max_output_tokens", request.options().maxOutputTokens());
        }
        if (request.options().reasoningEffort() != null
                && !request.options().reasoningEffort().isBlank()) {
            body.putObject("reasoning")
                    .put("effort", request.options().reasoningEffort());
        }

        if (request.outputContract().format() == OutputContract.Format.JSON) {
            if (!structuredOutputSupported) {
                throw new LlmProviderException(
                        providerId,
                        LlmProviderException.Kind.BAD_REQUEST,
                        null,
                        "Configured provider does not support structured output");
            }
            ObjectNode format = body.putObject("text").putObject("format");
            format.put("type", "json_schema");
            format.put("name", "canonical_output");
            format.put("strict", true);
            try {
                format.set("schema", objectMapper.readTree(request.outputContract().schemaJson()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON response schema", e);
            }
        }
        return body;
    }

    public CanonicalLlmResponse response(String providerId, String requestedModel, JsonNode root) {
        String status = text(root, "status");
        if ("failed".equals(status) || "cancelled".equals(status)) {
            throw new LlmProviderException(
                    providerId,
                    LlmProviderException.Kind.REJECTED,
                    null,
                    "OpenAI-compatible response ended with status " + status);
        }
        if ("incomplete".equals(status)) {
            String reason = text(root.path("incomplete_details"), "reason");
            throw new LlmProviderException(
                    providerId,
                    "max_output_tokens".equals(reason)
                            ? LlmProviderException.Kind.UNKNOWN
                            : LlmProviderException.Kind.REJECTED,
                    null,
                    "OpenAI-compatible response was incomplete"
                            + (reason == null || reason.isBlank() ? "" : ": " + reason));
        }

        List<String> parts = new ArrayList<>();
        List<String> refusals = new ArrayList<>();

        String topLevelOutputText = text(root, "output_text");
        if (topLevelOutputText != null && !topLevelOutputText.isBlank()) {
            parts.add(topLevelOutputText);
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                if (!"message".equals(text(item, "type"))) continue;
                JsonNode content = item.path("content");
                if (!content.isArray()) continue;
                for (JsonNode part : content) {
                    String type = text(part, "type");
                    if (("output_text".equals(type) || "text".equals(type))
                            && part.hasNonNull("text")) {
                        String value = part.get("text").asText();
                        if (!value.isBlank()) parts.add(value);
                    } else if ("refusal".equals(type)) {
                        String refusal = text(part, "refusal");
                        if (refusal == null || refusal.isBlank()) refusal = text(part, "text");
                        if (refusal != null && !refusal.isBlank()) refusals.add(refusal);
                    }
                }
            }
        }
        if (parts.isEmpty()) {
            if (!refusals.isEmpty()) {
                throw new LlmProviderException(
                        providerId,
                        LlmProviderException.Kind.REJECTED,
                        null,
                        "OpenAI-compatible response refused the request: "
                                + String.join(" ", refusals));
            }
            throw new LlmProviderException(
                    providerId,
                    LlmProviderException.Kind.MALFORMED_RESPONSE,
                    null,
                    "OpenAI-compatible response contained no output_text" + responseShape(root, status)
                            + (status == null || status.isBlank() ? "" : " (status=" + status + ")"));
        }

        JsonNode usage = root.path("usage");
        Integer inputTokens = integer(usage, "input_tokens");
        Integer outputTokens = integer(usage, "output_tokens");
        String model = text(root, "model");
        if (model == null || model.isBlank()) model = requestedModel;

        return new CanonicalLlmResponse(
                providerId,
                model,
                text(root, "id"),
                String.join("", parts),
                new CanonicalLlmResponse.Usage(inputTokens, outputTokens),
                status);
    }

    private static String responseShape(JsonNode root, String status) {
        List<String> outputTypes = new ArrayList<>();
        List<String> contentTypes = new ArrayList<>();
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                String itemType = text(item, "type");
                if (itemType != null) outputTypes.add(itemType);
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        String partType = text(part, "type");
                        if (partType != null) contentTypes.add(partType);
                    }
                }
            }
        }
        return " (status=" + status
                + ", outputTypes=" + outputTypes
                + ", contentTypes=" + contentTypes + ")";
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
