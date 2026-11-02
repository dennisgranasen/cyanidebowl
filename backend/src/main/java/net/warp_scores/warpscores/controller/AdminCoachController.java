package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Coach;
import net.warp_scores.warpscores.service.CoachService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/coaches")
@PreAuthorize(Authorities.AUTHORITY_WRITE_SITE_ADMIN)
public class AdminCoachController {
    private final CoachService coaches;

    @GetMapping
    public List<Coach> all() {
        return coaches.all();
    }

    @PutMapping("/{id}/mappings")
    public Coach updateMappings(@PathVariable String id, @RequestBody MappingRequest request) {
        return coaches.updateMappings(id, request.bb2CoachId(), request.bb3CoachId());
    }

    public record MappingRequest(String bb2CoachId, String bb3CoachId) {}
}
