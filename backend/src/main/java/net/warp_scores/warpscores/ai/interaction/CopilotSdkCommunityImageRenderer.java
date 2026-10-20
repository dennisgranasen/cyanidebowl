package net.warp_scores.warpscores.ai.interaction;

import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolBinaryResult;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolResultObject;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class CopilotSdkCommunityImageRenderer implements AiCommunityImageRenderer {
    private static final String TOOL_NAME = "generate_community_image";

    private final GeminiCommunityImageRenderer gemini;
    private final boolean enabled;
    private final String cliPath;
    private final Duration timeout;
    private final String githubToken;
    private final String copilotModel;

    CopilotSdkCommunityImageRenderer(
            GeminiCommunityImageRenderer gemini,
            boolean enabled,
            String cliPath,
            Duration timeout,
            String githubToken,
            String copilotModel) {
        this.gemini = gemini;
        this.enabled = enabled;
        this.cliPath = cliPath;
        this.timeout = timeout;
        this.githubToken = githubToken;
        this.copilotModel = copilotModel;
    }

    @Override
    public boolean isConfigured() {
        return enabled && gemini.isConfigured();
    }

    @Override
    public boolean supportsReferenceImages() {
        return isConfigured() && gemini.supportsReferenceImages();
    }

    String imageModel() {
        return copilotModel + " via " + gemini.imageModel();
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
        requireConfiguredProvider("copilot-sdk");
        AtomicReference<RenderedImage> generated = new AtomicReference<>();
        AtomicReference<Exception> generationFailure = new AtomicReference<>();
        ToolDefinition imageTool = imageTool(prompt, target, imageUrls, generated, generationFailure);

        try (CopilotClient client = new CopilotClient(clientOptions())) {
            client.start().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            SessionConfig sessionConfig = new SessionConfig()
                    .setModel(copilotModel)
                    .setAvailableTools(List.of())
                    .setTools(List.of(imageTool))
                    .setOnPermissionRequest((permissionRequest, invocation) ->
                            CompletableFuture.completedFuture(PermissionRequestResult.reject(
                                    "This image-generation session grants no external permissions.")))
                    .setEnableSessionTelemetry(false);
            var session = client.createSession(sessionConfig)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            session.sendAndWait(new MessageOptions().setPrompt(
                    "Call the " + TOOL_NAME + " tool exactly once to generate an image. "
                            + "The tool already has the exact saved image prompt and any reference images. "
                            + "Do not rewrite or expand the prompt. After the tool returns, finish."))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiCommunityImageProviderException(
                    "Copilot SDK image request was interrupted", true, null, null);
        } catch (TimeoutException timeoutFailure) {
            throw new AiCommunityImageProviderException(
                    "Copilot SDK image request exceeded " + timeout, true, 504, null);
        } catch (ExecutionException sdkFailure) {
            Throwable cause = sdkFailure.getCause() == null ? sdkFailure : sdkFailure.getCause();
            throw new AiCommunityImageProviderException(
                    "Copilot SDK image request failed: " + message(cause), false);
        }

        Exception toolFailure = generationFailure.get();
        if (toolFailure != null) throw toolFailure;
        RenderedImage image = generated.get();
        if (image == null) {
            throw new AiCommunityImageProviderException(
                    "Copilot SDK completed without invoking the image-generation tool", false);
        }
        return new RenderedImage(
                image.bytes(),
                image.contentType(),
                image.extension(),
                "copilot-sdk+gemini",
                imageModel());
    }

    ToolDefinition imageTool(
            String prompt,
            AiCommunityMediaGenerationRequest.Target target,
            List<String> imageUrls,
            AtomicReference<RenderedImage> generated,
            AtomicReference<Exception> generationFailure) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(),
                "additionalProperties", false);
        return ToolDefinition.createSkipPermission(
                TOOL_NAME,
                "Generate the image for the current community-media job using its saved prompt.",
                schema,
                invocation -> CompletableFuture.supplyAsync(() -> {
                    try {
                        RenderedImage image = gemini.renderWithReferences(prompt, target, imageUrls);
                        generated.set(image);
                        String data = Base64.getEncoder().encodeToString(image.bytes());
                        return new ToolResultObject(
                                "success",
                                "Image generated successfully.",
                                List.of(new ToolBinaryResult(
                                        data, image.contentType(), "image",
                                        "Generated community-media image")),
                                null,
                                null,
                                Map.of());
                    } catch (Exception failure) {
                        generationFailure.set(failure);
                        return ToolResultObject.error(message(failure));
                    }
                }));
    }

    private CopilotClientOptions clientOptions() {
        CopilotClientOptions options = new CopilotClientOptions();
        if (StringUtils.hasText(cliPath)) options.setCliPath(cliPath);
        if (StringUtils.hasText(githubToken)) {
            options.setGitHubToken(githubToken).setUseLoggedInUser(false);
        }
        return options;
    }

    private static String message(Throwable failure) {
        return StringUtils.hasText(failure.getMessage())
                ? failure.getMessage()
                : failure.getClass().getSimpleName();
    }
}
