package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/community/fans")
@RequiredArgsConstructor
public class AiCommunityFanController {
    private final AiCommunityMemberProfileRepository profiles;

    @GetMapping
    public List<AiCommunityMemberProfile> list(
            @RequestParam(required = false) String teamId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return profiles.findAllByOrderByDisplayNameAsc().stream()
                .filter(p -> includeInactive || p.isActive())
                .filter(p -> !StringUtils.hasText(teamId) || teamId.equals(p.getTeamId()))
                .sorted(Comparator.comparing(
                        AiCommunityMemberProfile::getDisplayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @GetMapping("/{id}")
    public AiCommunityMemberProfile get(@PathVariable String id) {
        return profiles.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Community fan profile not found"));
    }
}
