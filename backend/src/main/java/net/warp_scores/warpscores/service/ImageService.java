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

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {
    private final CyanideApiProperties cyanideApiProperties;

    private final ImageCacheRepository imageCacheRepository;

    @DurationLogging
    public Optional<byte[]> loadImage(String imageUrl, Optional<Integer> maxWidth) {
        Optional<ImageCache> imageCache = imageCacheRepository.findById(imageUrl);
        boolean cacheOutdated = imageCache.map(this::cacheOutdated).orElse(true);
        if (!cacheOutdated) {
            log.debug("Got image for url '{}' from cache.", imageUrl);
            return Optional.of(imageCache.get().getImageData());
        }
        var image = new Object() {
            Optional<byte[]> data = loadImageFromCyanide(imageUrl);
        };
        if (image.data.isEmpty()) {
            image.data = loadFromClassPath(imageUrl);
        }
        image.data = maxWidth.map(width -> rescaleImage(image.data, width)).orElse(image.data);
        image.data.ifPresent(bytes -> cacheImage(imageUrl, bytes));
        return image.data.isPresent() ? image.data : imageCache.map(ImageCache::getImageData);
    }

    private Optional<byte[]> loadImageFromCyanide(String imageUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] data = response.getBody();
                return Optional.ofNullable(data);
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

    private void cacheImage(String imageUrl, byte[] imageData) {
        ImageCache imageCache = new ImageCache();
        imageCache.setImageUrl(imageUrl);
        imageCache.setImageData(imageData);
        imageCache.setLastAccess(new Date());
        imageCacheRepository.save(imageCache);
        log.info("Stored image for url '{}' to cache.", imageUrl);
    }

    private boolean cacheOutdated(ImageCache imageCache) {
        Instant cacheInvalidAfter = Instant.now()
                .minus(Duration.ofMinutes(cyanideApiProperties.getImagesCache().getMaxValidityInMinutes()));
        return cacheInvalidAfter.isAfter(imageCache.getLastAccess().toInstant());
    }

}
