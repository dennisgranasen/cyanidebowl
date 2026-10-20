package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.ai.provider.cloudflare.CloudflareAiProviderProperties;
import net.warp_scores.warpscores.ai.provider.gemini.GeminiProviderProperties;
import net.warp_scores.warpscores.ai.provider.openai.OpenAiNativeProviderProperties;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeminiCommunityImageRendererTest {
    private static final String IMAGE_RESPONSE = """
            {
              "status": "completed",
              "model": "gemini-nano-banana-2.1",
              "output_image": {
                "data": "aW1hZ2U=",
                "mime_type": "image/png"
              }
            }
            """;

    @Test
    void sendsNanoBananaInteractionAndReturnsItsImage() throws Exception {
        var http = mock(HttpClient.class);
        response(http, 200, IMAGE_RESPONSE);
        var properties = properties();
        var json = new ObjectMapper();
        var renderer = new GeminiCommunityImageRenderer(json, properties, http);

        var image = renderer.render("A clear portrait", Target.AVATAR);

        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requests.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest request = requests.getValue();
        assertEquals("/v1beta/interactions", request.uri().getPath());
        assertEquals("test-key", request.headers().firstValue("x-goog-api-key").orElseThrow());
        var body = json.readTree(readBody(request));
        assertEquals("gemini-nano-banana-2.1", body.path("model").asText());
        assertEquals("A clear portrait", body.path("input").asText());
        assertFalse(body.path("store").asBoolean());
        assertEquals("image", new String(image.bytes(), StandardCharsets.UTF_8));
        assertEquals("image/png", image.contentType());
        assertEquals("png", image.extension());
        assertEquals("gemini", image.provider());
        assertEquals("gemini-nano-banana-2.1", image.model());
    }

    @Test
    void sendsReferenceImagesAsInlineInteractionContent(@TempDir Path tempDir) throws Exception {
        var reference = tempDir.resolve("reference.png");
        var source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(source, "png", reference.toFile());

        var http = mock(HttpClient.class);
        response(http, 200, IMAGE_RESPONSE);
        var renderer = new GeminiCommunityImageRenderer(
                new ObjectMapper(), properties(), http);

        renderer.renderWithReferences("Keep the same identity", Target.PROFILE_IMAGE,
                List.of(reference.toUri().toString()));

        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requests.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        var body = new ObjectMapper().readTree(readBody(requests.getValue()));
        assertEquals("text", body.path("input").get(0).path("type").asText());
        assertEquals("Keep the same identity", body.path("input").get(0).path("text").asText());
        var image = body.path("input").get(1);
        assertEquals("image", image.path("type").asText());
        assertEquals("image/png", image.path("mime_type").asText());
        assertTrue(java.util.Base64.getDecoder().decode(image.path("data").asText()).length > 0);
    }

    @Test
    void reportsInteractionRefusalWhenNoImageIsReturned() throws Exception {
        var http = mock(HttpClient.class);
        response(http, 200, """
                {"status":"failed","error":{"message":"The image request was blocked."}}
                """);
        var renderer = new GeminiCommunityImageRenderer(
                new ObjectMapper(), properties(), http);

        var failure = assertThrows(AiCommunityImageProviderException.class,
                () -> renderer.render("A portrait", Target.AVATAR));

        assertTrue(failure.getMessage().contains("returned no image"));
        assertTrue(failure.getMessage().contains("The image request was blocked."));
        assertFalse(failure.isRetryable());
    }

    @Test
    void marksGeminiQuotaErrorsAsRetryableQuotaExhaustion() throws Exception {
        var http = mock(HttpClient.class);
        var response = response(http, 429,
                "{\"error\":{\"message\":\"RESOURCE_EXHAUSTED: quota exceeded\"}}");
        when(response.headers()).thenReturn(
                java.net.http.HttpHeaders.of(
                        java.util.Map.of("Retry-After", List.of("120")),
                        (name, value) -> true));
        var renderer = new GeminiCommunityImageRenderer(
                new ObjectMapper(), properties(), http);

        var failure = assertThrows(AiCommunityImageProviderException.class,
                () -> renderer.render("A portrait", Target.AVATAR));

        assertEquals(429, failure.statusCode());
        assertTrue(failure.isRetryable());
        assertTrue(failure.quotaExhausted());
        assertNotNull(failure.retryAt());
    }

    @Test
    void exposesConfiguredNanoBananaAsASelectableCommunityProvider() {
        var gemini = properties();
        var providers = new CommunityImageProviders(
                new ObjectMapper(),
                new CloudflareAiProviderProperties(),
                new OpenAiNativeProviderProperties(),
                mock(LlmProviderRouter.class),
                gemini,
                new MockEnvironment(),
                true,
                "",
                java.time.Duration.ofMinutes(5),
                "",
                "gpt-6-luna");

        assertTrue(providers.available().stream()
                .anyMatch(provider -> provider.id().equals("gemini")
                        && provider.name().equals("Gemini Nano Banana")
                        && provider.configured()
                        && provider.referenceImages()));
        assertTrue(providers.available().stream()
                .anyMatch(provider -> provider.id().equals("copilot-sdk")
                        && provider.name().equals("Copilot SDK + Gemini Nano Banana")
                        && provider.configured()
                        && provider.referenceImages()));
        assertEquals(new AiCommunityImageRenderer.ProviderSelection(
                        "gemini", "gemini-nano-banana-2.1"),
                providers.selection(Target.AVATAR, "gemini", false));
        assertEquals(new AiCommunityImageRenderer.ProviderSelection(
                        "copilot-sdk", "gpt-6-luna via gemini-nano-banana-2.1"),
                providers.selection(Target.AVATAR, "copilot-sdk", false));
    }

    private static GeminiProviderProperties properties() {
        var properties = new GeminiProviderProperties();
        properties.setApiKey("test-key");
        return properties;
    }

    private static HttpResponse<String> response(
            HttpClient http,
            int status,
            String body) throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        return response;
    }

    private static String readBody(HttpRequest request) throws Exception {
        var body = new java.io.ByteArrayOutputStream();
        var completed = new CompletableFuture<String>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                body.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable failure) {
                completed.completeExceptionally(failure);
            }

            @Override
            public void onComplete() {
                completed.complete(body.toString(StandardCharsets.UTF_8));
            }
        });
        return completed.get(1, TimeUnit.SECONDS);
    }

}
