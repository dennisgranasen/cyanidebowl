package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.cyanide.api.model.common.Skill;
import net.warp_scores.warpscores.service.ImageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
            name = name.equals("null") ? null : String.format("Logo_%s", name);
        }
        Optional<String> imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getLogos(), name);
        return loadImage(imageUrl);
    }

    @GetMapping("/dbbc.png")
    public ResponseEntity<byte[]> getDbbcLogoPng() {
        return getDbbcLogoPng(null);
    }

    @GetMapping("/dbbc.png/{size}")
    public ResponseEntity<byte[]> getDbbcLogoPng(@PathVariable(name = "size", required = false) String size) {
        Optional<byte[]> imageData = Optional.empty();
        imageData = imageService.loadFromClassPath("/dbbc.png");
        int width = 256;
        if ("small".equalsIgnoreCase(size)) {
            width = 64;
        } else if ("medium".equalsIgnoreCase(size)) {
            width = 128;
        }
        imageData = imageService.rescaleImage(imageData, width);
        return imageData
                .map(ImageController::ok)
                .orElse(noContent());
    }

    @GetMapping("/warpscores.png")
    public ResponseEntity<byte[]> getWarpScoresLogoPng() {
        return getWarpScoresLogoPng(null);
    }

    @GetMapping("/warpscores.png/{size}")
    public ResponseEntity<byte[]> getWarpScoresLogoPng(@PathVariable(name = "size", required = false) String size) {
        Optional<byte[]> imageData = Optional.empty();
        if (size != null && !size.equalsIgnoreCase("original")) {
            imageData = imageService.loadFromClassPath("/warpscores.png");
            int width = 256;
            if ("small".equalsIgnoreCase(size)) {
                width = 64;
            } else if ("medium".equalsIgnoreCase(size)) {
                width = 128;
            }
            imageData = imageService.rescaleImage(imageData, width);
        } else {
            imageData = imageService.loadFromClassPath("/warpscores.800.png");
        }
        return imageData
                .map(ImageController::ok)
                .orElse(noContent());
    }

    @GetMapping("/warpscores.svg")
    public ResponseEntity<byte[]> getWarpScoresLogoSvg() {
        Optional<byte[]> imageData = imageService.loadFromClassPath("/warpscores.svg");
        return imageData
                .map(ImageController::ok)
                .orElse(noContent());
    }

    @GetMapping("/skill/{name}")
    public ResponseEntity<byte[]> getSkillImage(@PathVariable(name = "name") String name) {
        String imageName = translateSkillToImageName(name);
        Optional<String> imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getSkills(), imageName);
        return loadImage(imageUrl);
    }

    @GetMapping("/race/{name}")
    public ResponseEntity<byte[]> getRaceImage(@PathVariable(name = "name") String name) {
        String imageName = translateRaceToImageName(name);
        Optional<String> imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getRaces(), imageName);
        return loadImage(imageUrl, Optional.of(300));
    }

    @GetMapping("/stadium/{name}")
    public ResponseEntity<byte[]> getStadiumImage(@PathVariable(name = "name") String name) {
        Optional<String> imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getStadiums(), name);
        return loadImage(imageUrl, Optional.of(128));
    }

    @GetMapping("/portrait/{name}")
    public ResponseEntity<byte[]> getPortraitImage(@PathVariable(name = "name") String name) {
        Optional<String> imageUrl = getImageUrlFor(cyanideApiProperties.getUrls().getImages().getPortraits(), name);
        return loadImage(imageUrl);
    }

    private ResponseEntity<byte[]> loadImage(Optional<String> imageUrl) {
        return loadImage(imageUrl, Optional.empty());
    }

    private ResponseEntity<byte[]> loadImage(Optional<String> imageUrl, Optional<Integer> maxWidth) {
        Optional<byte[]> imageData = imageUrl.flatMap(url -> imageService.loadImage(url, maxWidth));
        return imageData
                .map(ImageController::ok)
                .orElse(noContent());
    }

    private static ResponseEntity<byte[]> ok(byte[] data) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(data);
    }

    private static ResponseEntity<byte[]> noContent() {
        return ResponseEntity.noContent().build();
    }

    private String translateSkillToImageName(String name) {
        return Skill.forCaseInsensitiveName(name).getImageName();
    }

    private String translateRaceToImageName(String name) {
        Race race = Race.valueOf(name);
        return "TeamScreenshot_" + race.getImageName();
    }

    private Optional<String> getImageUrlFor(String baseUrl, String name) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(name)) {
            return Optional.empty();
        }
        String ext = cyanideApiProperties.getUrls().getImagesExtension();
        return Optional.of(String.format("%s/%s%s%s", baseUrl, name,
                ext.startsWith(".") ? "" : ".",
                ext));
    }
}
