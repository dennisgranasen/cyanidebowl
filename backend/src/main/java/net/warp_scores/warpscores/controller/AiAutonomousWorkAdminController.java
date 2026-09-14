package net.warp_scores.warpscores.controller;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiCommunityMediaWorker;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.ai.provider.AiTargetExecutionQueueManager;
import net.warp_scores.warpscores.ai.scheduling.AiAutonomousWorkQueue;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/admin/ai-autonomous-work") @RequiredArgsConstructor @PreAuthorize("@userPermissionService.isSiteAdmin(authentication)")
public class AiAutonomousWorkAdminController {
    private final AiAutonomousWorkQueue queue; private final AiGenerationAdmissionService admission; private final AiTargetExecutionQueueManager executionQueues; private final AiCommunityMediaWorker mediaWorker; private final AiSettingsRepository settingsRepository;
    @GetMapping public Overview overview(){return new Overview(autonomousExecutionEnabled(),queue.snapshot(),admission.usageSnapshot(),executionQueues.snapshots(),mediaWorker.snapshot());}
    @PutMapping("/enabled") public Overview setEnabled(@RequestBody EnabledUpdate u){if(u==null||u.enabled()==null)throw new IllegalArgumentException("enabled is required");AiSettings s=settingsRepository.findById(AiSettings.GLOBAL_ID).orElseGet(AiSettings::new);s.setAutonomousExecutionEnabled(u.enabled());settingsRepository.save(s);return overview();}
    @PutMapping("/queue/jobs/{id}/priority") public Overview p1(@PathVariable String id,@RequestBody PriorityUpdate u){reqAutonomous(u);if(!queue.reprioritize(id,u.priority()))throw new IllegalArgumentException("Pending autonomous job not found");return overview();}
    @DeleteMapping("/queue/jobs/{id}") public Overview d1(@PathVariable String id){if(!queue.removePending(id))throw new IllegalArgumentException("Pending autonomous job not found");return overview();}
    @DeleteMapping("/queue/jobs") public Overview c1(){queue.clearPending();return overview();}
    @PutMapping("/execution-queues/{target}/jobs/{id}/priority") public Overview p2(@PathVariable String target,@PathVariable String id,@RequestBody PriorityUpdate u){req(u);if(!executionQueues.reprioritize(target,id,u.priority()))throw new IllegalArgumentException("Pending execution job not found");return overview();}
    @DeleteMapping("/execution-queues/{target}/jobs/{id}") public Overview d2(@PathVariable String target,@PathVariable String id){if(!executionQueues.remove(target,id))throw new IllegalArgumentException("Pending execution job not found");return overview();}
    @DeleteMapping("/execution-queues/{target}/jobs") public Overview c2(@PathVariable String target){executionQueues.clear(target);return overview();}
    @PutMapping("/media-queue/jobs/{id}/priority") public Overview p3(@PathVariable String id,@RequestBody PriorityUpdate u){req(u);if(!mediaWorker.reprioritize(id,u.priority()))throw new IllegalArgumentException("Pending media job not found");return overview();}
    @DeleteMapping("/media-queue/jobs/{id}") public Overview d3(@PathVariable String id){if(!mediaWorker.removePending(id))throw new IllegalArgumentException("Pending media job not found");return overview();}
    @DeleteMapping("/media-queue/jobs") public Overview c3(){mediaWorker.clearPending();return overview();}
    private boolean autonomousExecutionEnabled(){return settingsRepository.findById(AiSettings.GLOBAL_ID).map(AiSettings::isAutonomousExecutionEffectivelyEnabled).orElse(true);} private static void req(PriorityUpdate u){if(u==null||u.priority()==null||u.priority()<0||u.priority()>100)throw new IllegalArgumentException("priority must be 0..100");} private static void reqAutonomous(PriorityUpdate u){if(u==null||u.priority()==null||u.priority()<0||u.priority()>1000)throw new IllegalArgumentException("priority must be 0..1000");}
    public record EnabledUpdate(Boolean enabled){} public record PriorityUpdate(Integer priority){}
    public record Overview(boolean autonomousExecutionEnabled,AiAutonomousWorkQueue.QueueSnapshot queue,AiGenerationAdmissionService.UsageSnapshot generationUsage,java.util.List<AiTargetExecutionQueueManager.QueueSnapshot> executionQueues,AiCommunityMediaWorker.MediaQueueSnapshot mediaQueue){}
}
