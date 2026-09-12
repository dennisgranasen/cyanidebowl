package net.warp_scores.warpscores.ai.provider.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.LlmProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleProviderFactory {
    private final ObjectMapper objectMapper;
    private final OpenAiCompatibleProviderProperties properties;

    public List<LlmProvider> createProviders() {
        List<LlmProvider> providers = new ArrayList<>();
        for (Map.Entry<String, OpenAiCompatibleProviderProperties.Endpoint> entry
                : properties.getOpenaiCompatible().entrySet()) {
            providers.add(new OpenAiCompatibleLlmProvider(
                    entry.getKey(),
                    entry.getValue(),
                    objectMapper,
                    new OpenAiResponsesMapper(objectMapper)));
        }
        return List.copyOf(providers);
    }
}
