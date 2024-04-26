package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.model.common.Race;
import net.warp_scores.warpscores.cyanide.api.model.common.Skill;
import net.warp_scores.warpscores.service.ImageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/img")
public class ImageController {
    private final CyanideApiProperties cyanideApiProperties;

    private final ImageService imageService;

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

    private ResponseEntity<byte[]> loadImage(String imageUrl) {
        return loadImage(imageUrl, Optional.empty());
    }

    private ResponseEntity<byte[]> loadImage(String imageUrl, Optional<Integer> maxWidth) {
        Optional<byte[]> imageData = imageService.loadImage(imageUrl, maxWidth);
        return imageData
                .map(ImageController::okFor)
                .orElse(noContentFor(imageUrl));
    }

    @GetMapping("/portrait/{name}")
    public ResponseEntity<byte[]> getPortraitImage(@PathVariable(name = "name") String name) {
        String imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getPortraits(), name);
        return loadImage(imageUrl);
    }

    private static ResponseEntity<byte[]> okFor(byte[] data) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(data);
    }

    private static ResponseEntity<byte[]> noContentFor(String imageUrl) {
        log.error("No image found with url [{}]", imageUrl);
        return ResponseEntity.noContent().build();
    }

    private String translateSkillToImageName(String name) {
        return Skill.forCaseInsensitiveName(name).getImageName();
    }

    private String translateRaceToImageName(String name) {
        Race race = Race.valueOf(name);
        return "TeamScreenshot_" + race.getImageName();
    }

    private String getImageUrlFor(String baseUrl, String name) {
        return String.format("%s/%s.%s", baseUrl, name,
                cyanideApiProperties.getUrls().getImagesExtension());
    }
}
