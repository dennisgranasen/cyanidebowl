package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
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
    private final GeneratedMatchReportRepository reports;

    @GetMapping
    public Collection<PublicReporter> list() {
        return registry.enabled().stream().map(PublicReporter::from).toList();
    }

    @GetMapping("/{id}")
    public PublicReporter get(@PathVariable String id) {
        return PublicReporter.from(registry.require(id));
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
            String publicMarkdown) {

        static PublicReporter from(AiReporterDefinition d) {
            return new PublicReporter(
                    d.getId(),
                    d.getAlias(),
                    d.getRace(),
                    d.getCategory(),
                    d.getRole(),
                    d.getPortrait().getImage(),
                    d.getMarkdownBody());
        }
    }
}
