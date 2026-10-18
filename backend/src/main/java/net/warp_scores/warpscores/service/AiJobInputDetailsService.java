package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.EditorialArticleGenerationQueue;
import net.warp_scores.warpscores.ai.interaction.EditorialImageRequestService;
import net.warp_scores.warpscores.ai.provider.AiTargetExecutionQueueManager;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.reporting.DedicatedFanPlayerRatingJobService;
import net.warp_scores.warpscores.ai.scheduling.AiAutonomousWorkQueue;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiPlayerRatingJob;
import net.warp_scores.warpscores.model.AiPlayerRatingTask;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import net.warp_scores.warpscores.model.EditorialImageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiJobInputDetailsService {
    private final ObjectMapper json;
    private final MongoTemplate mongo;
    private final AiAutonomousWorkQueue autonomous;
    private final AiTargetExecutionQueueManager providerQueues;
    private final EditorialArticleGenerationQueue articles;
    private final EditorialImageRequestService editorialImages;
    private final DedicatedFanPlayerRatingJobService fanPlayerRatings;

    public JobInputDetails get(String type, String id, String queue) {
        return switch (type) {
            case "provider-call" -> providerCall(id, queue);
            case "autonomous" -> autonomous(id);
            case "article-generation" -> articleGeneration(id);
            case "editorial-image" -> editorialImage(id);
            case "community-media" -> communityMedia(id);
            case "fan-profile" -> fanProfile(id);
            case "player-rating" -> playerRating(id);
            case "fan-player-rating" -> fanPlayerRating(id);
            default -> throw new IllegalArgumentException("Unknown AI job input type: " + type);
        };
    }

    private JobInputDetails providerCall(String id, String queue) {
        if (queue == null || queue.isBlank()) {
            throw new IllegalArgumentException("queue is required for a provider call");
        }
        CanonicalLlmRequest request = providerQueues.requestForJob(queue, id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Provider input is no longer retained for this job"));
        return new JobInputDetails(
                "Provider call " + id,
                providerQueues.snapshots().stream()
                        .filter(snapshot -> Objects.equals(snapshot.target(), queue))
                        .map(snapshot -> snapshot.provider())
                        .findFirst().orElse(null),
                true,
                "This is the provider-neutral assembled request. A provider adapter may format it differently for its wire API.",
                List.of(
                        section("Request", requestMetadata(request)),
                        section("Task instruction", request.taskInstruction()),
                        section("Assembled context", pretty(request.context())),
                        section("Output contract", pretty(request.outputContract())),
                        section("Generation options", pretty(request.options()))));
    }

    private JobInputDetails autonomous(String id) {
        AiAutonomousWorkItem item = autonomous.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Autonomous job not found"));
        return details("Autonomous job " + id, null, true, null,
                section("Job payload", pretty(item)));
    }

    private JobInputDetails articleGeneration(String id) {
        try {
            JsonNode request = json.readTree(articles.requestJson(id));
            return details("Editorial article request " + id, null, true, null,
                    section("Saved article-generation request", pretty(request)));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Saved article-generation request is not valid JSON", failure);
        }
    }

    private JobInputDetails editorialImage(String id) {
        EditorialImageRequest request = editorialImages.get(id);
        return details("Editorial image request " + id, null, true, null,
                section("Prompt", request.getPrompt()),
                section("Photographer", request.getPhotographerId()),
                section("Article associations", pretty(request.getAssociations())),
                section("Reference images", request.isReferencesDisabled()
                        ? "References were disabled for this request."
                        : pretty(request.getReferenceImages())));
    }

    private JobInputDetails communityMedia(String id) {
        AiCommunityMediaGenerationRequest request = mongo.findById(id, AiCommunityMediaGenerationRequest.class);
        if (request == null) throw new IllegalArgumentException("Community-media job not found");
        String note = request.isInputReferenceSelectionRecorded()
                ? null
                : "The reference-image selection was not snapshotted for this older job, so its historical reference cannot be reconstructed reliably.";
        return details(
                "Community media " + id,
                request.getProvider(),
                request.isInputReferenceSelectionRecorded(),
                note,
                section("Prompt", request.getPrompt()),
                section("Target", request.getTarget()),
                section("Provider / model", Objects.toString(request.getProvider(), "Not recorded") + " / "
                        + Objects.toString(request.getModel(), "Not recorded")),
                section("Reference image URL used", request.isInputReferenceSelectionRecorded()
                        ? Objects.toString(request.getInputReferenceImageUrl(), "No reference image")
                        : "Not recorded"));
    }

    private JobInputDetails fanProfile(String id) {
        DedicatedFanReconciliationJob job = mongo.findById(id, DedicatedFanReconciliationJob.class);
        if (job == null) throw new IllegalArgumentException("Fan-profile job not found");
        return details(
                "Fan profile queue item " + id,
                null,
                false,
                "This is a pre-provider scheduling record. The assembled prompt and provider context are created by the worker and are not stored on this queue item.",
                section("Queue input", pretty(job)));
    }

    private JobInputDetails playerRating(String id) {
        AiPlayerRatingTask task = mongo.findById(id, AiPlayerRatingTask.class);
        if (task == null) throw new IllegalArgumentException("Player-rating task not found");
        AiPlayerRatingJob job = mongo.findById(task.getJobId(), AiPlayerRatingJob.class);
        return details(
                "Player-rating task " + id,
                null,
                false,
                "This is the saved task instruction and match selection. Detailed player context is assembled at execution and appears separately as a provider call.",
                section("Task", pretty(task)),
                section("Generation request", job == null ? "Parent request is no longer available." : pretty(job)));
    }

    private JobInputDetails fanPlayerRating(String id) {
        DedicatedFanPlayerRatingJobService.JobInputSnapshot input = fanPlayerRatings.inputSnapshot(id);
        if (input == null) throw new IllegalArgumentException("Fan player-rating job not found");
        return details(
                "Fan player-rating job " + id,
                null,
                false,
                "This job stores the match facts and instruction before generating one rating per fan. Each provider request is assembled as its worker runs.",
                section("Match facts", pretty(input.facts())),
                section("Instruction", Objects.toString(input.instruction(), "Not provided")),
                section("Options", pretty(new FanRatingOptions(input.matchId(), input.force(), input.fanNames()))));
    }

    private String requestMetadata(CanonicalLlmRequest request) {
        return "Agent: " + request.agentId()
                + "\nAgent version: " + Objects.toString(request.agentVersion(), "—")
                + "\nTask type: " + request.taskType()
                + "\nModel: " + request.model();
    }

    private String pretty(Object value) {
        if (value == null) return "Not recorded";
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not format saved AI job input", failure);
        }
    }

    private static JobInputDetails details(
            String title, String provider, boolean complete, String note, InputSection... sections) {
        return new JobInputDetails(title, provider, complete, note, List.of(sections));
    }

    private static InputSection section(String title, Object value) {
        String content = value == null
                ? "Not recorded"
                : value instanceof String text ? text : Objects.toString(value);
        return new InputSection(title, content);
    }

    public record JobInputDetails(
            String title,
            String provider,
            boolean complete,
            String note,
            List<InputSection> sections) {}

    public record InputSection(String title, String content) {}

    private record FanRatingOptions(String matchId, boolean force, List<String> fanNames) {}
}
