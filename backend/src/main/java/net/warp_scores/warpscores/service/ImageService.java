package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.cache.ImageCache;
import net.warp_scores.warpscores.domain.cache.ImageCacheRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {
    private static final int MAX_MEMORY_CACHE_ENTRIES = 1024;
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private final CyanideApiProperties cyanideApiProperties;
    private final ImageCacheRepository imageCacheRepository;

    private final ConcurrentMap<String, MemoryCacheEntry> memoryCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> loadLocks = new ConcurrentHashMap<>();

    @DurationLogging
    public Optional<byte[]> loadImage(String imageUrl, Optional<Integer> maxWidth) {
        String cacheKey = cacheKey(imageUrl, maxWidth);

        Optional<byte[]> memoryHit = getFromMemoryCache(cacheKey);
        if (memoryHit.isPresent()) {
            return memoryHit;
        }

        Object lock = loadLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        try {
            synchronized (lock) {
                memoryHit = getFromMemoryCache(cacheKey);
                if (memoryHit.isPresent()) {
                    return memoryHit;
                }

                Optional<ImageCache> imageCache = imageCacheRepository.findById(cacheKey);
                boolean cacheOutdated = imageCache.map(this::cacheOutdated).orElse(true);

                if (!cacheOutdated) {
                    byte[] data = imageCache.get().getImageData();
                    putInMemoryCache(cacheKey, data);
                    log.debug("Got image for key '{}' from persistent cache.", cacheKey);
                    return Optional.of(data);
                }

                Optional<byte[]> data = loadImageFromCyanide(imageUrl);
                if (data.isEmpty()) {
                    data = loadFromClassPath(imageUrl);
                }

                if (maxWidth.isPresent()) {
                    data = rescaleImage(data, maxWidth.get());
                }

                if (data.isPresent()) {
                    byte[] bytes = data.get();
                    cacheImage(cacheKey, bytes);
                    putInMemoryCache(cacheKey, bytes);
                    return Optional.of(bytes);
                }

                Optional<byte[]> stale = imageCache.map(ImageCache::getImageData);
                stale.ifPresent(bytes -> putInMemoryCache(cacheKey, bytes));
                return stale;
            }
        } finally {
            loadLocks.remove(cacheKey, lock);
        }
    }

    private Optional<byte[]> loadImageFromCyanide(String imageUrl) {
        try {
            ResponseEntity<byte[]> response = REST_TEMPLATE.getForEntity(imageUrl, byte[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (Exception ex) {
            log.error("Can't load image from cyanide (msg: {}).", ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<byte[]> loadFromClassPath(String imageUrl) {
        URI uri = URI.create(imageUrl);
        String path = String.format("img%s", uri.getPath());
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new FileNotFoundException(path);
            }
            byte[] data = in.readAllBytes();
            return Optional.of(data);
        } catch (IOException ex) {
            log.error("Can't load image from classpath (msg: {}).", ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<byte[]> rescaleImage(Optional<byte[]> imageData, int maxWidth) {
        if (imageData.isEmpty()) {
            return imageData;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(imageData.get());
        try {
            BufferedImage img = ImageIO.read(in);
            int height = (maxWidth * img.getHeight()) / img.getWidth();
            int width = (height * img.getWidth()) / img.getHeight();
            BufferedImage imageBuff = resizeImage(img, width, height);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(imageBuff, "png", buffer);
            return Optional.of(buffer.toByteArray());
        } catch (IOException e) {
            log.error("Unable to scale image, returning original image.");
            return imageData;
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

    private void cacheImage(String cacheKey, byte[] imageData) {
        ImageCache imageCache = new ImageCache();
        imageCache.setImageUrl(cacheKey);
        imageCache.setImageData(imageData);
        imageCache.setLastAccess(new Date());
        imageCacheRepository.save(imageCache);
        log.debug("Stored image for key '{}' in persistent cache.", cacheKey);
    }

    private Optional<byte[]> getFromMemoryCache(String cacheKey) {
        MemoryCacheEntry entry = memoryCache.get(cacheKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            memoryCache.remove(cacheKey, entry);
            return Optional.empty();
        }
        return Optional.of(entry.data());
    }

    private void putInMemoryCache(String cacheKey, byte[] imageData) {
        if (memoryCache.size() >= MAX_MEMORY_CACHE_ENTRIES) {
            evictExpiredMemoryEntries();
            if (memoryCache.size() >= MAX_MEMORY_CACHE_ENTRIES) {
                memoryCache.clear();
            }
        }

        memoryCache.put(
                cacheKey,
                new MemoryCacheEntry(
                        imageData,
                        Instant.now().plus(cacheDuration())
                )
        );
    }

    private void evictExpiredMemoryEntries() {
        Instant now = Instant.now();
        memoryCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private boolean cacheOutdated(ImageCache imageCache) {
        return Instant.now()
                .minus(cacheDuration())
                .isAfter(imageCache.getLastAccess().toInstant());
    }

    private Duration cacheDuration() {
        return Duration.ofMinutes(
                cyanideApiProperties.getImagesCache().getMaxValidityInMinutes()
        );
    }

    private static String cacheKey(String imageUrl, Optional<Integer> maxWidth) {
        return maxWidth
                .map(width -> imageUrl + "#width=" + width)
                .orElse(imageUrl);
    }

    private record MemoryCacheEntry(byte[] data, Instant expiresAt) {
    }
}
