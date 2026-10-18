package net.warp_scores.warpscores.ai.provider.copilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.CopilotClient;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.AssistantUsageEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SessionConfig;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmProvider;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import net.warp_scores.warpscores.ai.provider.ProviderCapabilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class GitHubCopilotLlmProvider implements LlmProvider {
    private final ObjectMapper json;
    private final boolean enabled;
    private final String cliPath;
    private final Duration timeout;
    private final String githubToken;

    public GitHubCopilotLlmProvider(
            ObjectMapper json,
            @Value("${warpscores.ai.providers.copilot.enabled:false}") boolean enabled,
            @Value("${warpscores.ai.providers.copilot.cli-path:}") String cliPath,
            @Value("${warpscores.ai.providers.copilot.timeout:5m}") Duration timeout,
            @Value("${warpscores.ai.providers.copilot.github-token:}") String githubToken) {
        this.json = json;
        this.enabled = enabled;
        this.cliPath = cliPath;
        this.timeout = timeout;
        this.githubToken = githubToken;
    }

    @Override
    public String id() {
        return "github-copilot";
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(null, null, false, false, false, false);
    }

    @Override
    public boolean isConfigured() {
        return enabled;
    }

    @Override
    public String configurationIssue() {
        if (!enabled) return "Set AI_COPILOT_ENABLED=true to enable GitHub Copilot.";
        return StringUtils.hasText(githubToken)
                ? null
                : "Configure AI_COPILOT_GITHUB_TOKEN or authenticate the Copilot runtime.";
    }

    @Override
    public CanonicalLlmResponse generate(CanonicalLlmRequest request) {
        if (!enabled) {
            throw new LlmProviderException(id(), LlmProviderException.Kind.UNAVAILABLE, 503,
                    "GitHub Copilot provider is disabled");
        }
        AtomicReference<String> content = new AtomicReference<>();
        AtomicLong inputTokens = new AtomicLong();
        AtomicLong outputTokens = new AtomicLong();
        AtomicBoolean usageReceived = new AtomicBoolean();
        try (CopilotClient client = new CopilotClient(clientOptions())) {
            client.start().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            SessionConfig sessionConfig = new SessionConfig()
                    .setModel(request.model())
                    .setAvailableTools(List.of())
                    .setEnableSessionTelemetry(false);
            if (request.options().reasoningEffort() != null) {
                sessionConfig.setReasoningEffort(request.options().reasoningEffort());
            }
            var session = client.createSession(sessionConfig).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            session.on(AssistantMessageEvent.class, message -> {
                if (message.getData().content() != null) content.set(message.getData().content());
            });
            session.on(AssistantUsageEvent.class, event -> {
                var usage = event.getData();
                if (usage.inputTokens() != null) inputTokens.addAndGet(usage.inputTokens());
                if (usage.outputTokens() != null) outputTokens.addAndGet(usage.outputTokens());
                usageReceived.set(true);
            });
            session.sendAndWait(new MessageOptions().setPrompt(prompt(request)))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException(id(), LlmProviderException.Kind.UNAVAILABLE, 503,
                    "GitHub Copilot request was interrupted", interrupted);
        } catch (TimeoutException timeoutFailure) {
            throw new LlmProviderException(id(), LlmProviderException.Kind.TIMEOUT, 504,
                    "GitHub Copilot request exceeded " + timeout, timeoutFailure);
        } catch (ExecutionException sdkFailure) {
            Throwable cause = sdkFailure.getCause() == null ? sdkFailure : sdkFailure.getCause();
            String failureMessage = message(cause);
            LlmProviderException.Kind kind = failureMessage.toLowerCase(java.util.Locale.ROOT)
                    .contains("auth") ? LlmProviderException.Kind.AUTHENTICATION : LlmProviderException.Kind.UNKNOWN;
            throw new LlmProviderException(id(), kind, null,
                    "GitHub Copilot SDK request failed: " + failureMessage, cause);
        }

        String response = content.get();
        if (!StringUtils.hasText(response)) {
            throw new LlmProviderException(id(), LlmProviderException.Kind.MALFORMED_RESPONSE, null,
                    "GitHub Copilot completed without an assistant response");
        }
        return new CanonicalLlmResponse(
                id(), request.model(), null, response,
                usageReceived.get()
                        ? new CanonicalLlmResponse.Usage(
                                tokenCount(inputTokens.get()), tokenCount(outputTokens.get()))
                        : CanonicalLlmResponse.Usage.unknown(),
                "completed");
    }

    public CopilotAccountQuota accountQuota() {
        if (!enabled) {
            throw new IllegalStateException("GitHub Copilot provider is disabled");
        }
        try (CopilotClient client = new CopilotClient(clientOptions())) {
            client.start().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            var result = client.getRpc().account.getQuota().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            var premium = result.quotaSnapshots().get("premium_interactions");
            if (premium == null) return null;
            return new CopilotAccountQuota(
                    Boolean.TRUE.equals(premium.isUnlimitedEntitlement())
                            || Long.valueOf(-1L).equals(premium.entitlementRequests()),
                    premium.entitlementRequests(),
                    premium.usedRequests(),
                    premium.remainingPercentage(),
                    premium.overage(),
                    premium.resetDate());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub Copilot quota request was interrupted", interrupted);
        } catch (TimeoutException timeoutFailure) {
            throw new IllegalStateException("GitHub Copilot quota request exceeded " + timeout, timeoutFailure);
        } catch (ExecutionException sdkFailure) {
            Throwable cause = sdkFailure.getCause() == null ? sdkFailure : sdkFailure.getCause();
            throw new IllegalStateException(
                    "GitHub Copilot quota request failed: " + message(cause), cause);
        }
    }

    private static Integer tokenCount(long tokens) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, tokens));
    }

    private CopilotClientOptions clientOptions() {
        CopilotClientOptions options = new CopilotClientOptions();
        if (StringUtils.hasText(cliPath)) options.setCliPath(cliPath);
        if (StringUtils.hasText(githubToken)) {
            options.setGitHubToken(githubToken).setUseLoggedInUser(false);
        }
        return options;
    }

    private String prompt(CanonicalLlmRequest request) {
        StringBuilder prompt = new StringBuilder(request.taskInstruction())
                .append("\n\nAssembled context:\n")
                .append(serialize(request.context()));
        if (!request.context().hardConstraints().isEmpty()) {
            prompt.append("\n\nHard constraints:\n")
                    .append(String.join("\n", request.context().hardConstraints()));
        }
        if (request.outputContract().format()
                == net.warp_scores.warpscores.ai.provider.OutputContract.Format.JSON) {
            prompt.append("\n\nReturn JSON matching this schema:\n")
                    .append(request.outputContract().schemaJson());
        }
        return prompt.toString();
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new LlmProviderException(id(), LlmProviderException.Kind.BAD_REQUEST, 400,
                    "Could not serialize the canonical Copilot request", failure);
        }
    }

    private static String message(Throwable failure) {
        return StringUtils.hasText(failure.getMessage())
                ? failure.getMessage()
                : failure.getClass().getSimpleName();
    }

    public record CopilotAccountQuota(
            Boolean unlimited,
            Long entitlementRequests,
            Long usedRequests,
            Double remainingPercentage,
            Double overage,
            OffsetDateTime resetDate) {
    }
}
