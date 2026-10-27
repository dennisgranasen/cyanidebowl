package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.service.MatchArticleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiMatchArticleWorkHandler implements AiAutonomousWorkHandler {
    private final MatchArticleService matchArticles;

    @Override
    public String handlerKey() {
        return AiCompletedMatchArticleWorkProducer.HANDLER_KEY;
    }

    @Override
    public void execute(AiAutonomousWorkItem item) {
        if (item == null
                || item.getKind() != AiAutonomousWorkItem.WorkKind.MATCH_ARTICLE
                || !StringUtils.hasText(item.getActorId())
                || !StringUtils.hasText(item.getTargetId())) {
            throw new IllegalArgumentException("Invalid match article work item");
        }
        String rawSlot = item.getPayload() == null ? null : item.getPayload().get("generationSlot");
        int generationSlot = rawSlot == null ? 0 : Integer.parseInt(rawSlot);
        matchArticles.requestAiAutonomously(item.getTargetId(), item.getActorId(), generationSlot);
    }
}