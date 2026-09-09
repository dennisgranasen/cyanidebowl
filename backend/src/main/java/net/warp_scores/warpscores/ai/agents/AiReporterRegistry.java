package net.warp_scores.warpscores.ai.agents;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiReporterRegistry {
    private final AiReporterProfileLoader loader;
    private Map<String, AiReporterDefinition> byId = Map.of();

    @PostConstruct
    void initialize() {
        byId = loader.loadAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        AiReporterDefinition::getId,
                        Function.identity()));
    }

    public Collection<AiReporterDefinition> all() {
        return byId.values();
    }

    public List<AiReporterDefinition> enabled() {
        return byId.values().stream()
                .filter(AiReporterDefinition::isEnabled)
                .toList();
    }

    public AiReporterDefinition require(String id) {
        AiReporterDefinition definition = byId.get(id);
        if (definition == null) {
            throw new NoSuchElementException("Unknown AI reporter: " + id);
        }
        return definition;
    }
}
