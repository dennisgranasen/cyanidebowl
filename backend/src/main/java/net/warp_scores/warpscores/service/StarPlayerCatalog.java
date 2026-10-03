package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class StarPlayerCatalog {
    public record Snapshot(String sourceCommit, List<Player> players) {}
    public record Player(String id, String name, String markdown, String sourceUrl, String imageUrl) {}

    private final List<Player> players;
    private final Map<String, Player> playersByKey;
    private final Map<String, Player> playersByUniqueNameToken;
    private final Path assetDir;

    @Autowired
    public StarPlayerCatalog(ObjectMapper json, Environment environment) throws IOException {
        try (var stream = new ClassPathResource("ai/starplayers.json").getInputStream()) {
            players = List.copyOf(json.readValue(stream, Snapshot.class).players());
        }
        playersByKey = new HashMap<>();
        Map<String, Player> uniqueNameTokens = new HashMap<>();
        Set<String> ambiguousNameTokens = new HashSet<>();
        for (Player player : players) {
            playersByKey.put(normalizeKey(player.id()), player);
            playersByKey.putIfAbsent(normalizeKey(player.name()), player);
            for (String token : player.name().split("[^A-Za-z0-9]+")) {
                String normalizedToken = normalizeKey(token);
                if (normalizedToken.length() < 4 || ambiguousNameTokens.contains(normalizedToken)) continue;
                Player previous = uniqueNameTokens.putIfAbsent(normalizedToken, player);
                if (previous != null && previous != player) {
                    uniqueNameTokens.remove(normalizedToken);
                    ambiguousNameTokens.add(normalizedToken);
                }
            }
        }
        playersByUniqueNameToken = Map.copyOf(uniqueNameTokens);
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

    public String displayName(String playerName) {
        if (playerName == null || playerName.isBlank()) return playerName;
        String key = normalizeKey(playerName);
        Player player = playersByKey.get(key);
        if (player == null) player = playersByUniqueNameToken.get(key);
        return player == null ? playerName : player.name();
    }

    private static String normalizeKey(String value) {
        String key = value.trim()
                .replaceFirst("(?i)_FALLBACK$", "")
                .replaceFirst("(?i)^PLAYER_NAMES_CHAMPION_", "")
                .replaceFirst("(?i)^name_sp_", "")
                .replaceFirst("(?i)^sp_", "");
        return key.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
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
        return Optional.ofNullable(player.imageUrl());
    }

    public Path assetDir() {
        return assetDir;
    }

}
