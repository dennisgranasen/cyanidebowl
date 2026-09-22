package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.EditorialImageRequestRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiSettings;
import net.warp_scores.warpscores.model.EditorialImageRequest;
import net.warp_scores.warpscores.service.ArticleImageSubjects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditorialImageRequestService {
    private final EditorialImageRequestRepository requests;
    private final AiSettingsRepository settings;
    private final ObjectProvider<AiCommunityImageRenderer> renderers;
    private final EditorialPhotographerRegistry photographers;
    private final AiCommunityMediaAssetStore assets;
    private final ArticleImageSubjects imageSubjects;

    public EditorialImageRequest create(String requestedBy, String photographerId, String prompt,
                                        java.util.List<net.warp_scores.warpscores.model.Article.Association> associations,
                                        java.util.List<String> references, boolean referencesDisabled) {
        EditorialImageRequest request = new EditorialImageRequest();
        request.setId(UUID.randomUUID().toString());
        request.setRequestedBy(requestedBy);
        request.setPhotographerId(photographers.require(photographerId).id());
        request.setPrompt(prompt);
        request.setAssociations(java.util.List.copyOf(associations));
        request.setReferenceImages(java.util.List.copyOf(references));
        request.setReferencesDisabled(referencesDisabled);
        request.setCreatedAt(Instant.now());
        request.setApprovalPolicy(policy());
        request.setStatus(request.getApprovalPolicy() == EditorialImageRequest.ApprovalPolicy.AUTO
                ? EditorialImageRequest.Status.DEVELOPING : EditorialImageRequest.Status.COMMISSIONED);
        return requests.save(request);
    }

    public EditorialImageRequest get(String id) {
        return requests.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown image request"));
    }

    public java.util.List<EditorialImageRequest> list() {
        return requests.findAll().stream()
                .sorted(java.util.Comparator.comparing(EditorialImageRequest::getCreatedAt).reversed())
                .toList();
    }

    public EditorialImageRequest review(String id, boolean approve, String reviewer, boolean technician) {
        EditorialImageRequest request = get(id);
        if (request.getStatus() != EditorialImageRequest.Status.COMMISSIONED) {
            throw new IllegalStateException("This image request is no longer awaiting approval");
        }
        if (request.getApprovalPolicy() == EditorialImageRequest.ApprovalPolicy.TECHNICIAN && !technician) {
            throw new org.springframework.security.access.AccessDeniedException("Technician approval required");
        }
        request.setApprovedBy(reviewer);
        request.setStatus(approve ? EditorialImageRequest.Status.DEVELOPING : EditorialImageRequest.Status.REJECTED);
        return requests.save(request);
    }

    @Scheduled(fixedDelayString = "${warpscores.ai.editorial-image.poll-ms:30000}")
    public synchronized void processOne() {
        EditorialImageRequest request = requests.findFirstByStatusOrderByCreatedAtAsc(
                EditorialImageRequest.Status.DEVELOPING).orElse(null);
        if (request == null) return;
        try {
            AiCommunityImageRenderer renderer = renderers.orderedStream()
                    .filter(AiCommunityImageRenderer::isConfigured).findFirst().orElse(null);
            if (renderer == null) return;
            String prompt = request.getPrompt() + "\n" + photographers.require(request.getPhotographerId()).imageDirection();
            boolean useReferences = !request.isReferencesDisabled()
                    && !request.getReferenceImages().isEmpty() && renderer.supportsReferenceImages();
            AiCommunityImageRenderer.RenderedImage image = useReferences
                    ? renderer.renderWithReferences(prompt, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE,
                            request.getReferenceImages())
                    : renderer.render(prompt, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE);
            String url = assets.save("article", "illustration", image.extension(), image.bytes()).publicUrl();
            request.setAssetUrl(url);
            request.setImageId(imageSubjects.save(url, request.getAssociations(), prompt));
            request.setStatus(EditorialImageRequest.Status.COMPLETED);
            request.setCompletedAt(Instant.now());
        } catch (Exception e) {
            request.setStatus(EditorialImageRequest.Status.FAILED);
            request.setError(e.getMessage());
            request.setCompletedAt(Instant.now());
        }
        requests.save(request);
    }

    private EditorialImageRequest.ApprovalPolicy policy() {
        AiSettings value = settings.findById(AiSettings.GLOBAL_ID).orElse(null);
        return value == null || value.getEditorialImageApprovalPolicy() == null
                ? EditorialImageRequest.ApprovalPolicy.EDITORIAL
                : value.getEditorialImageApprovalPolicy();
    }
}