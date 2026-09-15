package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.*;
import net.warp_scores.warpscores.model.*;
import net.warp_scores.warpscores.service.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles/tools")
public class EditorialArticleToolsController {
    private final EditorialArticleAiService ai;
    private final ArticleScopeService scopes;
    private final AiCommunityMediaAssetStore assets;
    private final ObjectProvider<AiCommunityImageRenderer> renderers;

    @GetMapping("/reporters")
    public List<EditorialArticleAiService.Reporter> reporters(Authentication auth, @RequestParam(required = false) String leagueSystemId) {
        return ai.reporters(auth, leagueSystemId);
    }
    @GetMapping("/policy")
    public EditorialArticleAiService.Policy policy(Authentication auth, @RequestParam(required = false) String leagueSystemId) {
        return ai.policy(auth, leagueSystemId);
    }
    public record PolicyInput(boolean autoAccept) {}
    @PutMapping("/policy")
    public EditorialArticleAiService.Policy policy(Authentication auth, @RequestParam(required = false) String leagueSystemId, @RequestBody PolicyInput input) {
        return ai.setPolicy(auth, leagueSystemId, input.autoAccept());
    }
    @PostMapping("/generate")
    public Article generate(Authentication auth, @RequestBody EditorialArticleAiService.Request input) throws Exception {
        return ai.generate(auth, input);
    }
    public record ImageInput(List<Article.Association> associations, String prompt, String title, String body) {}
    @PostMapping("/image")
    public Map<String, String> image(Authentication auth, @RequestBody ImageInput input) throws Exception {
        requireWriter(auth);
        var renderer = renderers.orderedStream().filter(AiCommunityImageRenderer::isConfigured).findFirst()
                .orElseThrow(() -> new IllegalStateException("Image generation is not configured"));
        String prompt = input.prompt();
        if (prompt == null || prompt.isBlank()) prompt = "Create an editorial illustration without lettering for this article:\n" + input.title() + "\n" + input.body();
        if (prompt.length() > 16000) prompt = prompt.substring(0, 16000);
        var image = renderer.render(prompt, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE);
        return Map.of("url", store(image.bytes()), "prompt", prompt);
    }
    @PostMapping("/upload")
    public Map<String, String> upload(Authentication auth, @RequestParam(required = false) String leagueSystemId,
                                      @RequestParam("file") MultipartFile file) throws Exception {
        requireWriter(auth);
        return Map.of("url", store(file.getBytes()));
    }
    private void requireWriter(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
    }

    private String store(byte[] bytes) throws Exception {
        if (bytes.length == 0 || bytes.length > 10 * 1024 * 1024) throw new IllegalArgumentException("Images must be smaller than 10 MB");
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Upload a PNG or JPEG image");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 20000000) throw new IllegalArgumentException("Image is too large");
                var image = reader.read(0);
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                return assets.save("article", "illustration", "png", output.toByteArray()).publicUrl();
            } finally { reader.dispose(); }
        }
    }
}
