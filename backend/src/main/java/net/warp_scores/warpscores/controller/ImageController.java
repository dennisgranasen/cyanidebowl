package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties.Images;
import net.warp_scores.warpscores.cyanide.api.model.common.Skill;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ImageService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/img")
public class ImageController {
    private final CyanideApiProperties cyanideApiProperties;

    private final ImageService imageService;
    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @GetMapping("/logo/{name}")
    public ResponseEntity<byte[]> getLogoImage(
            @PathVariable(name = "name") String name,
            @RequestParam(name = "opus", required = false) Integer opus) {
        if (name != null && !name.startsWith("Logo_")) {
            name = name.equals("null") ? null : String.format("Logo_%s", StringUtils.capitalize(name));
        } else if (name != null && name.startsWith("Logo_")) {
            name = "Logo_" + StringUtils.capitalize(name.substring("Logo_".length()));
        }

        Optional<String> imageUrl = 
            getImageUrlFor(cyanideApiProperties.getUrls().getImages().getLogos(),
                name, Optional.ofNullable(opus));
        //log.info("Image URL for logo: {}", imageUrl.orElse("null"));
        return loadImage(imageUrl);
    }

    @GetMapping("/dbbc.png")
    public ResponseEntity<byte[]> getDbbcLogoPng() {
        return getDbbcLogoPng(null);
    }

    @GetMapping("/dbbc.png/{size}")
    public ResponseEntity<byte[]> getDbbcLogoPng(@PathVariable(name = "size", required = false) String size) {
        Optional<byte[]> imageData;
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
        Optional<byte[]> imageData;
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
    public ResponseEntity<byte[]> getSkillImage(
            @PathVariable(name = "name") String name,
            @RequestParam(name = "opus", required = false) Integer opus) {
        String imageName = translateSkillToImageName(name);
        Images imgs = cyanideApiProperties.getUrls().getImages();
        if (opus == null) {
            opus = defaultOpus;
        }
        String dir;
        switch(opus){
            case 1:
                dir = imgs.getSkills1();
                break;
            case 2:
                dir = imgs.getSkills2();
                break;
            case 3:
            default:
                dir = imgs.getSkills3();
                break;
        }
        Optional<String> imageUrl = 
            getImageUrlFor(dir, imageName, Optional.ofNullable(opus));
        return loadImage(imageUrl);
    }

    @GetMapping("/race/{name}")
    public ResponseEntity<byte[]> getRaceImage(
            @PathVariable(name = "name") String name,
            @RequestParam(name = "opus", required = false) Integer opus) {
        String imageName = translateRaceToRaceImageName(name);

        Optional<String> imageUrl = 
            getImageUrlFor(cyanideApiProperties.getUrls().getImages().getRaces(), 
                imageName, Optional.ofNullable(opus));
        return loadImage(imageUrl, Optional.of(300));
    }

    @GetMapping("/stadium/{name}")
    public ResponseEntity<byte[]> getStadiumImage(
            @PathVariable(name = "name") String name,
            @RequestParam(name = "opus", required = false) Integer opus) {
        Optional<String> imageUrl = 
            getImageUrlFor(cyanideApiProperties.getUrls().getImages().getStadiums(),
                name, Optional.ofNullable(opus));
        return loadImage(imageUrl, Optional.of(128));
    }

    @GetMapping("/portrait/{name}")
    public ResponseEntity<byte[]> getPortraitImage(
            @PathVariable(name = "name") String name,
            @RequestParam(name = "opus", required = false) Integer opus) {
        Optional<String> imageUrl = 
            getImageUrlFor(cyanideApiProperties.getUrls().getImages().getPortraits(),
                name, Optional.ofNullable(opus));
        return loadImage(imageUrl);
    }


    private ResponseEntity<byte[]> loadImage(Optional<String> imageUrl) {
        //log.info(imageUrl.orElse("null image URL"));
        return loadImage(imageUrl, Optional.empty());
    }

    private ResponseEntity<byte[]> loadImage(Optional<String> imageUrl,
            Optional<Integer> maxWidth,
            Optional<String>... fallbackImageUrls) {
        Optional<byte[]> imageData = imageUrl.flatMap(url -> imageService.loadImage(url, maxWidth));
        for (int i = 0; i < fallbackImageUrls.length && imageData.isEmpty(); i++) {
            Optional<String> fallbackImageUrl = fallbackImageUrls[i];
            if (fallbackImageUrl.isPresent()) {
                imageData = fallbackImageUrl.flatMap(url -> imageService.loadImage(url, maxWidth));
            }
        }
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
        try {
            return Skill.forCaseInsensitiveName(name).map(Skill::getImageName).orElse(name.replaceAll(" ", ""));
        }
        catch (NoSuchElementException ex) {
            String imageName = name.replaceAll(" ", "");
            log.warn("No skill found for skill name '{}'. Returning original name as image name.", imageName);
            return imageName;
        }
        catch (IllegalArgumentException ex) {
            String imageName = name.replaceAll(" ", "");
            log.warn("Ambiguous skills found for skill name '{}'. Returning original name as image name.", imageName);
            return imageName;
        }    
    }

    private String translateRaceToRaceImageName(String name) {
        Race race = Race.valueOf(name.toLowerCase());
        return "TeamScreenshot_" + race.getImageName();
    }

    private Optional<String> getImageUrlFor(String baseUrl, String name, Optional<Integer> opus) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(name)) {
            return Optional.empty();
        }
        String ext = cyanideApiProperties.getUrls().getImagesExtension();
        String url = String.format("%s/%s%s%s", baseUrl, name,
                ext.startsWith(".") ? "" : ".",
                ext);
        return Optional.of(url.replace("{OPUS}", 
            opus.orElse(defaultOpus).toString()));
    }
}
