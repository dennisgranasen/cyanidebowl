package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Versioned BBBase rules and portrait references, available without a runtime GitHub dependency. */
@Service
public class StarPlayerCatalog {
    public record Player(String id, String name, String markdown, String sourceUrl, String imageUrl) {}
    public record Snapshot(String sourceCommit, List<Player> players) {}
    private final List<Player> players;

    public StarPlayerCatalog(ObjectMapper json) throws IOException {
        try (var stream = new ClassPathResource("ai/starplayers.json").getInputStream()) {
            players = List.copyOf(json.readValue(stream, Snapshot.class).players());
        }
    }
    public Player require(String id) {
        return players.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown star player: " + id));
    }
    public List<Player> search(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return players.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(q)).limit(30).toList();
    }
}
