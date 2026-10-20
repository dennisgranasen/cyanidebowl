package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.warp_scores.warpscores.ai.provider.RetryAfter;
import net.warp_scores.warpscores.ai.provider.gemini.GeminiProviderProperties;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public class GeminiCommunityImageRenderer implements AiCommunityImageRenderer {
    private final ObjectMapper objectMapper;
    private final GeminiProviderProperties properties;
    private final HttpClient httpClient;
    private final ImageReferenceSupport references = new ImageReferenceSupport();

    GeminiCommunityImageRenderer(
            ObjectMapper objectMapper,
            GeminiProviderProperties properties) {
        this(objectMapper, properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    }

    GeminiCommunityImageRenderer(
            ObjectMapper objectMapper,
            GeminiProviderProperties properties,
            HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getApiKey());
    }

    @Override
    public boolean supportsReferenceImages() {
        return isConfigured();
    }

    String imageModel() {
        return properties.getImageModel();
    }

    @Override
    public RenderedImage render(
            String prompt,
            AiCommunityMediaGenerationRequest.Target target) throws Exception {
        return renderWithReferences(prompt, target, List.of());
    }

    @Override
    public RenderedImage renderWithReferences(
            String prompt,
            AiCommunityMediaGenerationRequest.Target target,
            List<String> imageUrls) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini image generation requires an API key");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Image prompt is required");
        }

        List<ImageReferenceSupport.Loaded> loaded =
                references.load(imageUrls, 4);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getImageModel());
        body.put("store", false);
        if (loaded.isEmpty()) {
            body.put("input", prompt);
        } else {
            ArrayNode input = body.putArray("input");
            input.addObject().put("type", "text").put("text", prompt);
            for (ImageReferenceSupport.Loaded image : loaded) {
                input.addObject()
                        .put("type", "image")
                        .put("mime_type", image.contentType())
                        .put("data", Base64.getEncoder().encodeToString(image.bytes()));
            }
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.interactionsUrl()))
                .timeout(properties.getImageTimeout())
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw httpFailure(
                    statusCode,
                    response.body(),
                    response.headers().firstValue("Retry-After").orElse(null));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode image = imageOutput(root);
        if (image == null) {
            String status = root.path("status").asText("");
            throw new AiCommunityImageProviderException(
                    "Gemini image generation returned no image"
                            + (status.isBlank() ? "" : " (interaction status: " + status + ")")
                            + errorSuffix(root),
                    false);
        }

        String encoded = image.path("data").asText("");
        String contentType = image.path("mime_type").asText(
                image.path("mimeType").asText(""));
        if (!StringUtils.hasText(encoded)) {
            throw new AiCommunityImageProviderException(
                    "Gemini image generation returned empty image data", false);
        }
        String extension = extensionFor(contentType);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException invalidImage) {
            throw new AiCommunityImageProviderException(
                    "Gemini image generation returned invalid base64 image data", false);
        }
        if (bytes.length == 0) {
            throw new AiCommunityImageProviderException(
                    "Gemini image generation returned an empty image", false);
        }

        String model = root.path("model").asText(properties.getImageModel());
        return new RenderedImage(bytes, contentType, extension, "gemini", model);
    }

    private AiCommunityImageProviderException httpFailure(
            int status,
            String body,
            String retryAfter) {
        String detail = truncate(body, 700);
        String lowerBody = body == null ? "" : body.toLowerCase(Locale.ROOT);
        boolean quotaExhausted = status == 429
                && (lowerBody.contains("quota") || lowerBody.contains("resource_exhausted"));
        boolean retryable = status == 408 || status == 429 || status >= 500;
        String message = "Gemini image generation failed with HTTP " + status
                + (detail.isBlank() ? "" : ": " + detail);
        return new AiCommunityImageProviderException(
                message,
                retryable,
                status,
                RetryAfter.parse(retryAfter, java.time.Instant.now()),
                quotaExhausted);
    }

    private JsonNode imageOutput(JsonNode root) {
        JsonNode direct = root.path("output_image");
        if (hasImageData(direct)) return direct;

        JsonNode steps = root.path("steps");
        if (steps.isArray()) {
            for (JsonNode step : steps) {
                if (!"model_output".equals(step.path("type").asText())) continue;
                JsonNode image = imageInContent(step.path("content"));
                if (image != null) return image;
            }
        }
        return imageInContent(root.path("output"));
    }

    private JsonNode imageInContent(JsonNode content) {
        if (!content.isArray()) return null;
        for (JsonNode item : content) {
            if ("image".equals(item.path("type").asText()) && hasImageData(item)) {
                return item;
            }
        }
        return null;
    }

    private boolean hasImageData(JsonNode image) {
        return image.isObject()
                && image.path("data").isTextual()
                && StringUtils.hasText(image.path("data").asText());
    }

    private String errorSuffix(JsonNode root) {
        JsonNode error = root.path("error");
        String message = error.path("message").asText("");
        if (!message.isBlank()) return ": " + message;

        JsonNode errors = root.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            message = errors.get(0).path("message").asText("");
            if (!message.isBlank()) return ": " + message;
        }
        return "";
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> throw new AiCommunityImageProviderException(
                    "Gemini image generation returned unsupported image type: "
                            + (contentType.isBlank() ? "not recorded" : contentType),
                    false);
        };
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
