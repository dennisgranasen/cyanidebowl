package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.ai.provider.openai.OpenAiNativeProviderProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OpenAiCommunityImageRenderer implements AiCommunityImageRenderer {
    private static final URI ENDPOINT =
            URI.create("https://api.openai.com/v1/images/generations");

    private final ObjectMapper objectMapper;
    private final OpenAiNativeProviderProperties providerProperties;
    private final HttpClient httpClient;
    private final String quality;

    public OpenAiCommunityImageRenderer(
            ObjectMapper objectMapper,
            OpenAiNativeProviderProperties providerProperties,
            org.springframework.core.env.Environment environment) {
        this.objectMapper = objectMapper;
        this.providerProperties = providerProperties;
        this.quality = environment.getProperty(
                "warpscores.ai.community-media.quality",
                "low");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(providerProperties.getApiKey());
    }

    @Override
    public RenderedImage render(
            String prompt,
            AiCommunityMediaGenerationRequest.Target target) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("warpscores.ai.providers.openai.api-key is not configured");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", providerProperties.getImageModel());
        body.put("prompt", prompt);
        body.put(
                "size",
                target == AiCommunityMediaGenerationRequest.Target.AVATAR
                        ? "1024x1024"
                        : "1536x1024");
        body.put("quality", quality);
        body.put("output_format", "png");

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(providerProperties.getTimeout())
                .header("Authorization", "Bearer " + providerProperties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "OpenAI image generation failed with HTTP "
                            + response.statusCode() + ": "
                            + truncate(response.body(), 500));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode encoded = root.path("data").path(0).path("b64_json");
        if (!encoded.isTextual() || !StringUtils.hasText(encoded.asText())) {
            throw new IllegalStateException(
                    "OpenAI image generation returned no data[0].b64_json");
        }

        return new RenderedImage(
                Base64.getDecoder().decode(encoded.asText()),
                "image/png",
                "png",
                "openai",
                providerProperties.getImageModel());
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
