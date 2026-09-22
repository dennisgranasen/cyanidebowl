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
    private final EditorialImageRequestService imageRequests;
    private final UserPermissionService permissions;

    public record ImageStatusInput(List<Article.Association> associations) {}
    public record ImageStatus(String provider, boolean blocked, boolean quotaExhausted,
                              java.time.Instant retryAt, String message,
                              int referenceImagesAvailable) {}

    @PostMapping("/image-status")
    public ImageStatus imageStatus(Authentication auth, @RequestBody ImageStatusInput input) {
        requireWriter(auth);
        var subjects = subjectContext.resolve(input.associations());
        var status = imageProviders().status(!subjects.referenceImages().isEmpty());
        return new ImageStatus(status.provider(), status.blocked(), status.quotaExhausted(),
                status.retryAt(), status.message(), subjects.referenceImages().size());
    }

    @ExceptionHandler(AiCommunityImageProviderException.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> imageProviderFailure(
            AiCommunityImageProviderException error) {
        int status = error.statusCode() == null ? 503 : error.statusCode();
        if (error.quotaExhausted() || status == 429) status = 429;
        var body = new java.util.LinkedHashMap<String, Object>();
        boolean safetyRejected = error.statusCode() != null
                && error.statusCode() == 400
                && error.getMessage() != null
                && error.getMessage().contains("/ code 3030");
        body.put("code", safetyRejected ? "IMAGE_SAFETY_REJECTED"
                : error.quotaExhausted() ? "IMAGE_QUOTA_EXHAUSTED"
                : "IMAGE_PROVIDER_ERROR");
        body.put("message", safetyRejected
                ? "Cloudflare rejected this prompt/reference-image combination."
                : error.quotaExhausted()
                    ? "Image provider quota or credits are exhausted"
                    : error.getMessage());
        body.put("quotaExhausted", error.quotaExhausted());
        body.put("retryable", error.isRetryable());
        if (error.retryAt() != null) body.put("retryAt", error.retryAt().toString());
        return org.springframework.http.ResponseEntity.status(status).body(body);
    }

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
    public record ImageInput(List<Article.Association> associations, String prompt, String title, String body,
                             String matchId, String reporterId, String photographerId,
                             Boolean ignoreReferences) {
        public ImageInput(List<Article.Association> associations, String prompt, String title, String body,
                          String matchId, String reporterId, String photographerId) {
            this(associations, prompt, title, body, matchId, reporterId, photographerId, false);
        }

        public ImageInput(List<Article.Association> associations, String prompt, String title, String body,
                          String matchId, String reporterId) {
            this(associations, prompt, title, body, matchId, reporterId, null, false);
        }

        public boolean referencesDisabled() {
            return Boolean.TRUE.equals(ignoreReferences);
        }
    }
    @PostMapping("/image")
    public Map<String, String> image(Authentication auth, @RequestBody ImageInput input) {
        requireWriter(auth);
        if (input.matchId() == null || input.matchId().isBlank()) {
            if (!permissions.canEditLeagueSystem(auth, null)) {
                throw new org.springframework.security.access.AccessDeniedException("Editorial permission required");
            }
        }
        photographers.require(input.photographerId());
        var subjects = subjectContext.resolve(input.associations());
        if (subjects.referenceImages().size() > 16) throw new IllegalArgumentException("At most 16 star player portraits per image");
        String prompt = imagePrompt(input) + "\nTAGGED SUBJECT CONTEXT:\n" + subjects.text();
        if (input.matchId() != null && !input.matchId().isBlank()) {
            var context = matchArticles.imageContext(auth, input.matchId(), input.reporterId());
            prompt += "\nAUTHORITATIVE MATCH EVIDENCE:\n" + context.evidence().json()
                    + "\nHISTORICAL COMPETITION CONTEXT:\n" + context.history().json();
        }
        return view(imageRequests.create(auth.getName(), input.photographerId(), prompt,
                subjects.associations(), subjects.referenceImages(), input.referencesDisabled()));
    }

    @GetMapping("/image/{id}")
    public Map<String, String> imageRequest(Authentication auth, @PathVariable String id) {
        requireWriter(auth);
        return view(imageRequests.get(id));
    }

    public record ReviewInput(boolean approve) {}
    @PostMapping("/image/{id}/review")
    public Map<String, String> reviewImageRequest(Authentication auth, @PathVariable String id,
                                                   @RequestBody ReviewInput input) {
        requireWriter(auth);
        boolean technician = permissions.isSiteAdmin(auth);
        if (!technician && !permissions.canEditLeagueSystem(auth, null)) {
            throw new org.springframework.security.access.AccessDeniedException("Editorial permission required");
        }
        return view(imageRequests.review(id, input.approve(), auth.getName(), technician));
    }

    private static Map<String, String> view(EditorialImageRequest request) {
        String text = switch (request.getStatus()) {
            case COMMISSIONED -> "Uppdraget är mottaget och väntar på godkännande.";
            case DEVELOPING -> "Agenten framkallar bilden.";
            case COMPLETED -> "Bilden är klar.";
            case FAILED -> "Bilduppdraget misslyckades.";
            case REJECTED -> "Bilduppdraget avslogs.";
        };
        var result = new java.util.LinkedHashMap<String, String>();
        result.put("id", request.getId());
        result.put("status", request.getStatus().name());
        result.put("statusText", text);
        result.put("photographerId", request.getPhotographerId());
        if (request.getAssetUrl() != null) result.put("url", request.getAssetUrl());
        if (request.getImageId() != null) result.put("imageId", request.getImageId());
        if (request.getError() != null) result.put("error", request.getError());
        return result;
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

    private CommunityImageProviders imageProviders() {
    return renderers.orderedStream()
            .filter(CommunityImageProviders.class::isInstance)
            .map(CommunityImageProviders.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "Community image provider status is not available"));
}
}
