package net.warp_scores.warpscores.ai.interaction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class AiCommunityMediaAssetStore {
    private final Path storageDir;

    public AiCommunityMediaAssetStore(
            @Value("${warpscores.ai.community-media.storage-dir:./data/community-media}")
            String storageDir) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    public StoredAsset save(
            String fanProfileId,
            String target,
            String extension,
            byte[] bytes) throws IOException {
        Files.createDirectories(storageDir);

        String filename = sanitize(fanProfileId)
                + "-" + sanitize(target.toLowerCase())
                + "-" + UUID.randomUUID()
                + "." + sanitize(extension);

        Path file = storageDir.resolve(filename).normalize();
        if (!file.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid media filename");
        }

        Files.write(
                file,
                bytes,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);

        return new StoredAsset(
                filename,
                "/community/media/assets/" + filename);
    }

    public Resource resource(String filename) {
        if (!StringUtils.hasText(filename)
                || !filename.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid media filename");
        }

        Path file = storageDir.resolve(filename).normalize();
        if (!file.startsWith(storageDir) || !Files.isRegularFile(file)) {
            return null;
        }
        return new FileSystemResource(file);
    }

    private static String sanitize(String value) {
        if (!StringUtils.hasText(value)) return "asset";
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public record StoredAsset(String filename, String publicUrl) {}
}
