package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StarPlayerCatalog {
    public record Snapshot(String sourceCommit, List<Player> players) {}
    public record Player(String id, String name, String markdown, String sourceUrl, String imageUrl) {}

    private final List<Player> players;
    private final Path assetDir;

    @Autowired
    public StarPlayerCatalog(ObjectMapper json, Environment environment) throws IOException {
        try (var stream = new ClassPathResource("ai/starplayers.json").getInputStream()) {
            players = List.copyOf(json.readValue(stream, Snapshot.class).players());
        }
        assetDir = Path.of(environment.getProperty(
                "warpscores.ai.star-players.asset-dir",
                environment.getProperty("STAR_PLAYER_ASSET_DIR", "data/starplayers")))
                .toAbsolutePath().normalize();
    }

    public StarPlayerCatalog(ObjectMapper json) throws IOException {
        this(json, new StandardEnvironment());
    }

    public Player require(String id) {
        return players.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown star player: " + id));
    }

    public List<Player> search(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return players.stream()
                .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(q))
                .limit(30)
                .toList();
    }

    public Optional<String> referenceImage(String id) {
        Player player = require(id);
        Path path = assetDir.resolve("references").resolve(safeId(player.id()) + ".png").normalize();
        if (!path.startsWith(assetDir) || !Files.isRegularFile(path)) return Optional.empty();
        return Optional.of(path.toUri().toString());
    }

    public Path assetDir() {
        return assetDir;
    }

    private static String safeId(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
    }
}
