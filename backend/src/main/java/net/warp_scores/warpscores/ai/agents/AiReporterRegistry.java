package net.warp_scores.warpscores.ai.agents;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReporterRegistry {
    private final AiReporterProfileLoader loader;
    private final AiReporterProfileValidator validator;
    private Map<String, AiReporterDefinition> byId = Map.of();
    private List<AiReporterDefinition> all = List.of();

    @PostConstruct
    void initialize() {
        List<AiReporterDefinition> loaded = loader.loadAll();
        validator.validateAll(loaded);
        all = loaded.stream().sorted(Comparator.comparing(AiReporterDefinition::getAlias, String.CASE_INSENSITIVE_ORDER)).toList();
        byId = all.stream().collect(Collectors.toUnmodifiableMap(AiReporterDefinition::getId, Function.identity()));
        if (byId.isEmpty()) {
            log.warn("No AI reporter profiles were loaded from classpath:ai_agents/reporters/*.md");
        } else {
            log.info("Loaded and validated {} canonical AI reporter profiles (schema v{})", byId.size(), AiReporterDefinition.CURRENT_SCHEMA_VERSION);
        }
    }

    public List<AiReporterDefinition> all() { return all; }
    public List<AiReporterDefinition> enabled() { return all.stream().filter(AiReporterDefinition::isEnabled).toList(); }
    public Optional<AiReporterDefinition> find(String id) { return Optional.ofNullable(byId.get(id)); }
    public boolean contains(String id) { return byId.containsKey(id); }
    public AiReporterDefinition require(String id) {
        return find(id).orElseThrow(() -> new NoSuchElementException("Unknown AI reporter: " + id));
    }
}
