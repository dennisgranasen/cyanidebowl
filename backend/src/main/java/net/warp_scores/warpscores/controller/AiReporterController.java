package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.domain.persistence.GeneratedMatchReportRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/ai-reporters")
@RequiredArgsConstructor
public class AiReporterController {
    private static final Pattern H2_SECTION =
            Pattern.compile("(?ms)^##\\s+(.+?)\\s*$\\R?(.*?)(?=^##\\s+|\\z)");

    private static final Set<String> INTERNAL_PUBLIC_SECTIONS = Set.of(
            "llm guidance",
            "portrait brief"
    );

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

    private static String publicMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        Matcher matcher = H2_SECTION.matcher(markdown);
        StringBuilder result = new StringBuilder();

        int firstSectionStart = -1;
        while (matcher.find()) {
            if (firstSectionStart < 0) {
                firstSectionStart = matcher.start();
                result.append(markdown, 0, matcher.start());
            }

            String heading = matcher.group(1).trim();
            if (!INTERNAL_PUBLIC_SECTIONS.contains(heading.toLowerCase())) {
                result.append("## ").append(heading).append("\n\n");
                result.append(matcher.group(2).trim()).append("\n\n");
            }
        }

        if (firstSectionStart < 0) {
            return markdown.trim();
        }

        return result.toString().trim();
    }

    public record PublicReporter(
            String id,
            String alias,
            String race,
            String category,
            String role,
            String portraitImage,
            String avatarImage,
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
                        d.getPortrait().getAvatar(),
                        AiReporterController.publicMarkdown(d.getMarkdownBody()),
                        active);
            }
    }
}
