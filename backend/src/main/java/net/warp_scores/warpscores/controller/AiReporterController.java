package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.domain.persistence.GeneratedMatchReportRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/ai-reporters")
@RequiredArgsConstructor
public class AiReporterController {
    private final AiReporterRegistry registry;
    private final AiReporterEffectiveProfileService effectiveProfiles;
    private final GeneratedMatchReportRepository reports;

    @GetMapping
    public Collection<PublicReporter> list() {
        return registry.all().stream()
                .map(definition -> PublicReporter.from(
                        definition, effectiveProfiles.effective(definition).enabled()))
                .filter(PublicReporter::active)
                .toList();
    }

    @GetMapping("/{id}")
    public PublicReporter get(@PathVariable String id) {
        var definition = registry.require(id);
        return PublicReporter.from(
                definition, effectiveProfiles.effective(definition).enabled());
    }

    @GetMapping("/{id}/reports")
    public List<?> reports(@PathVariable String id) {
        registry.require(id);
        return reports.findByReporterIdOrderByPublishedAtDesc(id);
    }

    public record PublicReporter(
            String id,
            String alias,
            String race,
            String category,
            String role,
            String portraitImage,
            String publicMarkdown,
            boolean active) {

        static PublicReporter from(AiReporterDefinition d, boolean active) {
            return new PublicReporter(
                    d.getId(),
                    d.getAlias(),
                    d.getRace(),
                    d.getCategory(),
                    d.getRole(),
                    d.getPortrait().getImage(),
                    d.getMarkdownBody(),
                    active);
        }
    }
}
