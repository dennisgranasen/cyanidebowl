package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiCommunityMediaAssetStore;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/community/media/assets")
@RequiredArgsConstructor
public class AiCommunityMediaAssetController {
    private final AiCommunityMediaAssetStore assets;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> asset(@PathVariable String filename) {
        Resource resource = assets.resource(filename);
        if (resource == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
