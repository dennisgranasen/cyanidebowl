package net.warp_scores.discord_bot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class VersionController {

    @GetMapping(value = "/version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> getVersion() {
        Optional<byte[]> versionData = loadVersionFile();
        return versionData
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Optional<byte[]> loadVersionFile() {
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("/version.json")) {
            if (in != null) {
                byte[] data = in.readAllBytes();
                return Optional.of(data);
            }
        } catch (IOException ex) {
            log.error("Can't load version.json from classpath (msg: {}).", ex.getMessage());
        }
        return Optional.of(
                "{ \"project\": \"net.warp-scores:warpscores-discord-bot\", \"committish\": \"n/a\", \"projectVersion\": \"dev\", \"timestamp\": \"n/a\" }".getBytes());
    }
}
