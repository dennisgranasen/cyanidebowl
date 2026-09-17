package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.openai.OpenAiNativeProviderProperties;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Target;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAiImageReferenceTest {
    @Test void portraitsAreImageInputsAndTextOnlyRequestsRetainGenerationEndpoint() throws Exception {
        var http = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"data\":[{\"b64_json\":\"aW1hZ2U=\"}]}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var properties = new OpenAiNativeProviderProperties(); properties.setApiKey("test-key");
        var json = new ObjectMapper();
        var renderer = new OpenAiCommunityImageRenderer(json, properties, new MockEnvironment(), http);
        var urls = List.of("https://example.test/morg.jpg", "https://example.test/griff.jpg");
        renderer.renderWithReferences("Morg and Griff at a fountain", Target.PROFILE_IMAGE, urls);
        renderer.render("A Blood Bowl stadium", Target.PROFILE_IMAGE);
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(2)).send(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertEquals("/v1/images/edits", requests.getAllValues().getFirst().uri().getPath());
        var body = json.readTree(readBody(requests.getAllValues().getFirst()));
        assertEquals(urls.getFirst(), body.path("images").get(0).path("image_url").asText());
        assertEquals(urls.get(1), body.path("images").get(1).path("image_url").asText());
        assertEquals(properties.getImageModel(), body.path("model").asText());
        assertTrue(body.path("prompt").asText().contains("Morg and Griff"));
        assertEquals("/v1/images/generations", requests.getAllValues().get(1).uri().getPath());
        assertFalse(json.readTree(readBody(requests.getAllValues().get(1))).has("images"));
    }
    private String readBody(HttpRequest request) throws Exception {
        var body = new java.io.ByteArrayOutputStream();
        var completed = new CompletableFuture<String>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); body.writeBytes(bytes); }
            public void onError(Throwable failure) { completed.completeExceptionally(failure); }
            public void onComplete() { completed.complete(body.toString(StandardCharsets.UTF_8)); }
        });
        return completed.get(1, TimeUnit.SECONDS);
    }
}
