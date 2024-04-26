package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.model.common.Race;
import net.warp_scores.warpscores.cyanide.api.model.common.Skill;
import net.warp_scores.warpscores.domain.cache.ImageCache;
import net.warp_scores.warpscores.domain.cache.ImageCacheRepository;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/img")
public class ImageController {
    private final CyanideApiProperties cyanideApiProperties;

    private final ImageCacheRepository imageCacheRepository;

    @GetMapping("/logo/{name}")
    public ResponseEntity<byte[]> getLogoImage(@PathVariable(name = "name") String name) {
        if (!name.startsWith("Logo_")) {
            name = String.format("Logo_%s", name);
        }
        String imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getLogos(), name);
        return loadImage(imageUrl);
    }

    @GetMapping("/skill/{name}")
    public ResponseEntity<byte[]> getSkillImage(@PathVariable(name = "name") String name) {
        String imageName = translateSkillToImageName(name);
        String imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getSkills(), imageName);
        return loadImage(imageUrl);
    }

    private String translateSkillToImageName(String name) {
        return Skill.forCaseInsensitiveName(name).getImageName();
    }

    private String translateRaceToImageName(String name) {
        Race race = Race.valueOf(name);
        return "TeamScreenshot_" + race.getImageName();
    }

    @GetMapping("/race/{name}")
    public ResponseEntity<byte[]> getRaceImage(@PathVariable(name = "name") String name) {
        String imageName = translateRaceToImageName(name);
        String imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getRaces(), imageName);
        return loadImage(imageUrl, Optional.of(300));
    }

    @GetMapping("/stadium/{name}")
    public ResponseEntity<byte[]> getStadiumImage(@PathVariable(name = "name") String name) {
        String imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getStadiums(), name);
        return loadImage(imageUrl, Optional.of(128));
    }

    @GetMapping("/portrait/{name}")
    public ResponseEntity<byte[]> getPortraitImage(@PathVariable(name = "name") String name) {
        String imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getPortraits(), name);
        return loadImage(imageUrl);
    }

    private String getImageUrlFor(String baseUrl, String name) {
        return String.format("%s/%s.%s", baseUrl, name,
                cyanideApiProperties.getUrls().getImagesExtension());
    }

    private ResponseEntity<byte[]> loadImage(String imageUrl) {
        return loadImage(imageUrl, Optional.empty());
    }

    private ResponseEntity<byte[]> loadImage(String imageUrl, Optional<Integer> maxWidth) {
        Optional<byte[]> imageData = loadImageFromCacheCyanideOrClasspath(imageUrl, maxWidth);
        return imageData
                .map(ImageController::okFor)
                .orElse(noContentFor(imageUrl));
    }

    private Optional<byte[]> loadImageFromCacheCyanideOrClasspath(String imageUrl, Optional<Integer> maxWidth) {
        Optional<ImageCache> imageCache = imageCacheRepository.findById(imageUrl);
        boolean cacheOutdated = imageCache.map(this::cacheOutdated).orElse(true);
        if (!cacheOutdated) {
            log.debug("Got image for url '{}' from cache.", imageUrl);
            return Optional.of(imageCache.get().getImageData());
        }
        var ref = new Object() {
            Optional<byte[]> image = loadImageFromCyanide(imageUrl);
        };
        if (ref.image.isEmpty()) {
            ref.image = loadFromClassPath(imageUrl);
        }
        ref.image = maxWidth.flatMap(width -> rescaleImage(ref.image, width));
        ref.image.ifPresent(bytes -> cacheImage(imageUrl, bytes));
        return ref.image;
    }

    private Optional<byte[]> loadImageFromCyanide(String imageUrl) {
        try {
            RestTemplate restTemplate = new RestTemplateBuilder().build();
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

    private Optional<byte[]> loadFromClassPath(String imageUrl) {
        URI uri = URI.create(imageUrl);
        String path = String.format("img%s", uri.getPath());
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                byte[] data = in.readAllBytes();
                return Optional.of(data);
            }
        } catch (Exception ex) {
            log.error("Can't load image from classpath (msg: {}).", ex.getMessage());
        }
        return Optional.empty();
    }

    private Optional<byte[]> rescaleImage(Optional<byte[]> imageData, int maxWidth) {
        if (imageData.isEmpty()) {
            return Optional.empty();
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

    private static ResponseEntity<byte[]> okFor(byte[] data) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(data);
    }

    private static ResponseEntity<byte[]> noContentFor(String imageUrl) {
        log.error("No image found with url {}.", imageUrl);
        return ResponseEntity.noContent().build();
    }

}
