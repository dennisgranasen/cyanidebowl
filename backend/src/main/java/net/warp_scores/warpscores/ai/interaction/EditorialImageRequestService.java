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
import java.util.Comparator;
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

    public record QueueEstimate(int ahead, int running, Long estimatedWaitSeconds, Instant notBefore) {}

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
                .sorted(Comparator.comparingInt(EditorialImageRequestService::priority).reversed()
                        .thenComparing(EditorialImageRequest::getCreatedAt))
                .toList();
    }

    public java.util.Optional<Instant> estimatedStartAt(String id) {
        java.util.List<EditorialImageRequest> active = list().stream()
                .filter(request -> request.getStatus() == EditorialImageRequest.Status.RUNNING
                        || request.getStatus() == EditorialImageRequest.Status.DEVELOPING)
                .toList();
        for (int index = 0; index < active.size(); index++) {
            EditorialImageRequest request = active.get(index);
            if (!request.getId().equals(id)) continue;
            if (request.getStatus() == EditorialImageRequest.Status.RUNNING) {
                return java.util.Optional.ofNullable(request.getStartedAt());
            }
            long running = active.stream()
                    .filter(item -> item.getStatus() == EditorialImageRequest.Status.RUNNING)
                    .count();
            long ahead = active.subList(0, index).stream()
                    .filter(item -> item.getStatus() == EditorialImageRequest.Status.DEVELOPING)
                    .count();
            return java.util.Optional.of(Instant.now().plusSeconds(30 + (running + ahead) * 60));
        }
        return java.util.Optional.empty();
    }

    public QueueEstimate queueEstimate(int priority) {
        java.util.List<EditorialImageRequest> active = list().stream()
                .filter(request -> request.getStatus() == EditorialImageRequest.Status.RUNNING
                        || request.getStatus() == EditorialImageRequest.Status.DEVELOPING)
                .toList();
        int ahead = (int) active.stream()
                .filter(request -> request.getStatus() == EditorialImageRequest.Status.DEVELOPING
                        && priority(request) >= priority)
                .count();
        int running = (int) active.stream()
                .filter(request -> request.getStatus() == EditorialImageRequest.Status.RUNNING)
                .count();
        return new QueueEstimate(ahead, running, 30L + (long) (ahead + running) * 60, null);
    }

    public synchronized boolean reprioritize(String id, int priority) {
        if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority must be 0..100");
        EditorialImageRequest request = get(id);
        if (request.getStatus() != EditorialImageRequest.Status.COMMISSIONED
                && request.getStatus() != EditorialImageRequest.Status.DEVELOPING) {
            return false;
        }
        request.setPriority(priority);
        requests.save(request);
        return true;
    }

    public synchronized boolean remove(String id) {
        EditorialImageRequest request = get(id);
        if (request.getStatus() == EditorialImageRequest.Status.RUNNING) return false;
        requests.delete(request);
        return true;
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
        EditorialImageRequest request = requests.findByStatusOrderByCreatedAtAsc(
                        EditorialImageRequest.Status.DEVELOPING).stream()
                .sorted(Comparator.comparingInt(EditorialImageRequestService::priority).reversed()
                        .thenComparing(EditorialImageRequest::getCreatedAt))
                .findFirst()
                .orElse(null);
        if (request == null) return;
        AiCommunityImageRenderer renderer = renderers.orderedStream()
                .filter(AiCommunityImageRenderer::isConfigured).findFirst().orElse(null);
        if (renderer == null) return;
        request.setStatus(EditorialImageRequest.Status.RUNNING);
        request.setStartedAt(Instant.now());
        request.setAttempts(request.getAttempts() + 1);
        requests.save(request);
        try {
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
            String message = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            request.setError(message.length() > 700 ? message.substring(0, 700) : message);
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

    private static int priority(EditorialImageRequest request) {
        return request.getPriority() == null ? 100 : request.getPriority();
    }
}