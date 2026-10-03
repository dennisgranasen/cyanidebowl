package net.warp_scores.warpscores.ai.context;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.StarPlayerCatalog;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StarPlayerPromptNormalizer {
    private static final Pattern STAR_PLAYER_KEY = Pattern.compile(
            "(?i)(?<![A-Za-z0-9])(?:PLAYER_NAMES_CHAMPION_|name_sp_|sp_)"
                    + "[A-Za-z0-9]+(?:_[A-Za-z0-9]+)*(?:_FALLBACK)?(?![A-Za-z0-9])");

    private final StarPlayerCatalog catalog;

    public String displayName(String playerName) {
        return catalog.displayName(playerName);
    }

    public String normalizeText(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher matcher = STAR_PLAYER_KEY.matcher(text);
        StringBuffer normalized = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(displayName(matcher.group())));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    public AssembledContext normalize(AssembledContext context) {
        if (context == null) return null;
        Map<ContextSection, List<ContextItem>> sections = new LinkedHashMap<>();
        context.sections().forEach((section, items) -> sections.put(section, items.stream()
                .map(this::normalize)
                .toList()));
        return new AssembledContext(
                context.worldModelVersion(),
                context.hardConstraints().stream().map(this::normalizeText).toList(),
                sections,
                context.estimatedTokens(),
                context.droppedItems());
    }

    private ContextItem normalize(ContextItem item) {
        return new ContextItem(
                item.id(),
                item.contentType(),
                item.source(),
                item.authority(),
                item.authorUserId(),
                item.authorSubject(),
                normalizeText(item.authorDisplayName()),
                item.timestamp(),
                item.thread(),
                item.replyToId(),
                item.subjects(),
                item.mentions(),
                item.references().stream().map(this::normalizeText).toList(),
                normalizeText(item.title()),
                normalizeText(item.body()),
                item.generation());
    }
}