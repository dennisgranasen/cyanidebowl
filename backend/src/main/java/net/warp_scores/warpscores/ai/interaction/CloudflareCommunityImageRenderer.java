package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.ai.provider.RetryAfter;
import net.warp_scores.warpscores.ai.provider.cloudflare.CloudflareAiProviderProperties;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CloudflareCommunityImageRenderer implements AiCommunityImageRenderer {
    private static final String API_ROOT = "https://api.cloudflare.com/client/v4/accounts/";

    private final ObjectMapper objectMapper;
    private final CloudflareAiProviderProperties properties;
    private final LlmProviderRouter routing;
    private final HttpClient httpClient;
    private final ImageReferenceSupport references = new ImageReferenceSupport();

    public CloudflareCommunityImageRenderer(ObjectMapper objectMapper,
                                            CloudflareAiProviderProperties properties,
                                            LlmProviderRouter routing) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.routing = routing;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getAccountId()) && StringUtils.hasText(properties.getApiKey());
    }

    @Override
    public boolean supportsReferenceImages() {
        if (!isConfigured()) return false;
        try {
            return requiresMultipart(selectedModel(AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public RenderedImage render(String prompt, AiCommunityMediaGenerationRequest.Target target) throws Exception {
        return renderInternal(prompt, target, List.of());
    }

    @Override
    public RenderedImage renderWithReferences(String prompt,
                                              AiCommunityMediaGenerationRequest.Target target,
                                              List<String> imageReferences) throws Exception {
        if (imageReferences == null || imageReferences.isEmpty()) return render(prompt, target);
        String model = selectedModel(target);
        if (!requiresMultipart(model))
            throw new IllegalArgumentException("Configured Cloudflare image model does not support reference images: " + model);
        return renderInternal(prompt, target, imageReferences);
    }

    private RenderedImage renderInternal(String prompt,
                                         AiCommunityMediaGenerationRequest.Target target,
                                         List<String> imageReferences) throws Exception {
        if (!isConfigured())
            throw new IllegalStateException("Cloudflare Workers AI requires account-id and api-key");

        String selectedModel = selectedModel(target);
        URI endpoint = endpoint(selectedModel);
        HttpRequest request;

        if (requiresMultipart(selectedModel)) {
            var loaded = references.load(imageReferences, 4);
            String boundary = "----cyanidebowl-" + UUID.randomUUID();
            byte[] multipart = multipartBody(boundary, prompt, target, loaded);
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
                    .build();
        } else {
            if (!imageReferences.isEmpty())
                throw new IllegalArgumentException("Reference images require a FLUX.2 Cloudflare model");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", prompt);
            body.put("steps", Math.max(1, Math.min(8, properties.getImageSteps())));
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        }

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw providerFailure(response.statusCode(), response.body(),
                    response.headers().firstValue("Retry-After").orElse(null));

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode encoded = root.path("result").path("image");
        if (!encoded.isTextual() || !StringUtils.hasText(encoded.asText()))
            throw new AiCommunityImageProviderException("Cloudflare Workers AI returned no result.image", false);

        return new RenderedImage(Base64.getDecoder().decode(encoded.asText()),
                "image/jpeg", "jpg", "cloudflare", selectedModel);
    }

    private String selectedModel(AiCommunityMediaGenerationRequest.Target target) {
        ContextTaskType taskType = target == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE
                ? ContextTaskType.PROFILE_IMAGE : ContextTaskType.AVATAR_IMAGE;
        var plan = routing.planForTask("community-media", taskType, LlmProviderRouter.ExecutionOverrides.none());
        if (!"cloudflare".equals(plan.primary().providerId()))
            throw new IllegalStateException("Community image target must use cloudflare");
        return plan.primary().model();
    }

    private URI endpoint(String selectedModel) {
        String encodedModel = URLEncoder.encode(selectedModel, StandardCharsets.UTF_8)
                .replace("%2F", "/").replace("%40", "@");
        return URI.create(API_ROOT + properties.getAccountId() + "/ai/run/" + encodedModel);
    }

    private boolean requiresMultipart(String model) {
        return model != null && (model.contains("/flux-2-dev") || model.contains("/flux-2-klein-"));
    }

    private byte[] multipartBody(String boundary, String prompt,
                                 AiCommunityMediaGenerationRequest.Target target,
                                 List<ImageReferenceSupport.Loaded> images) throws Exception {
        var out = new ByteArrayOutputStream();
        writeField(out, boundary, "prompt", prompt);
        writeField(out, boundary, "width",
                target == AiCommunityMediaGenerationRequest.Target.AVATAR ? "1024" : "1536");
        writeField(out, boundary, "height", "1024");

        for (int i = 0; i < images.size(); i++) {
            var image = images.get(i);
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"input_image_" + i + "\"; filename=\"" + image.filename() + "\"\r\n"
                    + "Content-Type: " + image.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(image.bytes());
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private AiCommunityImageProviderException providerFailure(int statusCode, String body, String retryAfter) {
        String internalCode = extractCloudflareCode(body);
        boolean quotaExhausted = statusCode == 429 && "4006".equals(internalCode);
        boolean retryable = statusCode == 408 || statusCode >= 500
                || (statusCode == 429 && "3040".equals(internalCode));
        if (statusCode == 429 && "3036".equals(internalCode)) retryable = false;

        java.time.Instant retryAt = RetryAfter.parse(retryAfter, java.time.Instant.now());
        if (quotaExhausted && (retryAt == null || !retryAt.isAfter(java.time.Instant.now()))) {
            retryAt = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                    .toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        }

        return new AiCommunityImageProviderException(
                "Cloudflare Workers AI failed with HTTP " + statusCode
                        + (internalCode == null ? "" : " / code " + internalCode) + ": "
                        + truncate(body, 700),
                retryable, statusCode, retryAt, quotaExhausted);
    }

    private String extractCloudflareCode(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode errors = root.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                JsonNode code = errors.get(0).path("code");
                if (!code.isMissingNode() && !code.isNull()) return code.asText();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
