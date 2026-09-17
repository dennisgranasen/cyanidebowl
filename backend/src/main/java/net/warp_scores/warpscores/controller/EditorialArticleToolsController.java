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
    private final MatchArticleService matchArticles;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final AiCommunityMediaAssetStore assets;
    private final ObjectProvider<AiCommunityImageRenderer> renderers;
    private final net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry photographers;
    private final ArticleImagePromptService imagePrompts;
    private final EditorialSubjectContext subjectContext;
    private final ArticleImageSubjects imageSubjects;

    @GetMapping("/photographers")
    public List<net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry.Photographer> photographers() {
        return photographers.all();
    }

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
    public record ImageInput(List<Article.Association> associations, String prompt, String title, String body, String matchId, String reporterId, String photographerId) {
        public ImageInput(List<Article.Association> associations, String prompt, String title, String body, String matchId, String reporterId) {
            this(associations, prompt, title, body, matchId, reporterId, null);
        }
    }
    @PostMapping("/image")
    public Map<String, String> image(Authentication auth, @RequestBody ImageInput input) throws Exception {
        requireWriter(auth);
        var photographer = photographers.require(input.photographerId());
        var renderer = renderers.orderedStream().filter(AiCommunityImageRenderer::isConfigured).findFirst()
                .orElseThrow(() -> new IllegalStateException("Image generation is not configured"));
        var subjects = subjectContext.resolve(input.associations());
        if (subjects.referenceImages().size() > 16) throw new IllegalArgumentException("At most 16 star player portraits per image");
        String prompt = imagePrompt(input) + "\nTAGGED SUBJECT CONTEXT:\n" + subjects.text();
        net.warp_scores.warpscores.ai.context.AssembledContext assembled = null;
        if (input.matchId() != null && !input.matchId().isBlank()) {
            var context = matchArticles.imageContext(auth, input.matchId(), input.reporterId());
            // The same assembly, evidence projection and history used by match-report generation.
            assembled = context.assembled();
            prompt += "\nAUTHORITATIVE MATCH EVIDENCE:\n" + context.evidence().json()
                    + "\nHISTORICAL COMPETITION CONTEXT:\n" + context.history().json();
        }
        String brief = imagePrompts.prepare(prompt, photographer, assembled, !subjects.associations().isEmpty());
        boolean useReferences = !subjects.referenceImages().isEmpty() && renderer.supportsReferenceImages();
        var image = !useReferences
                ? renderer.render(brief, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE)
                : renderer.renderWithReferences(brief + "\nReference portraits in order: "
                    + subjects.associations().stream().filter(a -> a.type() == Article.LinkType.STAR_PLAYER)
                        .map(a -> a.id().replace('_', ' ')).collect(java.util.stream.Collectors.joining("; ")),
                    AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE, subjects.referenceImages());
        String url = store(image.bytes());
        String imageId = imageSubjects.save(url, subjects.associations(), brief);
        return Map.of("url", url, "imageId", imageId, "prompt", imagePrompt(input),
                "photographerId", photographer.id(), "photographerName", photographer.alias(),
                "referenceImagesUsed", Boolean.toString(useReferences),
                "referenceImagesAvailable", Boolean.toString(!subjects.referenceImages().isEmpty()));
    }
    @PostMapping("/upload")
    public Map<String, String> upload(Authentication auth, @RequestParam(required = false) String leagueSystemId,
                                      @RequestParam(required = false) String associations,
                                      @RequestParam("file") MultipartFile file) throws Exception {
        requireWriter(auth);
        List<Article.Association> links = associations == null || associations.isBlank()
                ? List.of()
                : json.readValue(associations,
                    json.getTypeFactory().constructCollectionType(List.class, Article.Association.class));
        var subjects = subjectContext.resolve(links);
        String url = store(file.getBytes());
        String imageId = imageSubjects.save(url, subjects.associations(), "Uploaded editorial image");
        return Map.of("url", url, "imageId", imageId);
    }
    static String imagePrompt(ImageInput input) {
        String direction = input.prompt() == null || input.prompt().isBlank()
                ? "Create an editorial illustration without lettering." : input.prompt().trim();
        return direction + "\nArticle title:\n" + (input.title() == null ? "" : input.title())
                + "\nArticle text (editorial perspective, not independently verified facts):\n"
                + (input.body() == null ? "" : input.body());
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
