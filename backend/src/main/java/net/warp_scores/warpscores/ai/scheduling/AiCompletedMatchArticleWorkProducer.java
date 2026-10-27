package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiCompletedMatchArticleWorkProducer {
    public static final String HANDLER_KEY = "match-article-generation";

    private final StageSourceRepository stageSources;
    private final ReplayAnalysisRepository replayAnalyses;
    private final AiInitiativePolicyService initiativePolicy;
    private final net.warp_scores.warpscores.service.MatchArticleService matchArticles;
    private final AiAutonomousWorkQueue queue;

    public boolean onMatchFinalized(Match match) {
        if (!isEligibleFinalizedMatch(match)) return false;

        String matchId = canonicalMatchId(match);
        if (!replayAnalyses.existsById(matchId)) return false;
        String leagueSystemId = null;
        for (StageSource source : stageSources.findBySourceEntityId(match.getCompetitionId())) {
            if (source != null && StringUtils.hasText(source.getLeagueSystemId())) {
                leagueSystemId = source.getLeagueSystemId().trim();
                break;
            }
        }
        if (leagueSystemId == null || !initiativePolicy.staffMayRunAutonomously(
                leagueSystemId, AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE)) {
            return false;
        }
        var staffPolicy = initiativePolicy.effective(leagueSystemId).staff();
        var reporters = matchArticles.autonomousReporterIds(leagueSystemId);
        int articleCount = staffPolicy.getMatchArticlesPerMatch();
        if (reporters.isEmpty() || articleCount < 1 || articleCount > 10) return false;

        boolean queuedAny = false;
        for (int slot = 0; slot < articleCount; slot++) {
            String reporterId = selectDeterministically(matchId, slot, reporters);
            queuedAny |= queue.enqueue(new AiAutonomousWorkQueue.EnqueueRequest(
                    candidateKey(matchId, slot),
                    HANDLER_KEY,
                    AiAutonomousWorkItem.WorkKind.MATCH_ARTICLE,
                    AiAutonomousWorkItem.Priority.AUTONOMOUS,
                    leagueSystemId,
                    reporterId,
                    "MATCH",
                    matchId,
                    java.util.Map.of("generationSlot", Integer.toString(slot)),
                    3));
        }
        return queuedAny;
    }

    static String candidateKey(String matchId, int slot) {
        return "match-article:" + matchId + ":" + slot;
    }

    static String selectDeterministically(String matchId, int slot, java.util.List<String> reporterIds) {
        int hash = (matchId + "|" + slot).hashCode();
        return reporterIds.get(Math.floorMod(hash, reporterIds.size()));
    }

    private static boolean isEligibleFinalizedMatch(Match match) {
        return match != null
                && Boolean.TRUE.equals(match.getIsFinalized())
                && match.getCompetitionId() != null
                && StringUtils.hasText(canonicalMatchId(match))
                && match.getTeams() != null
                && match.getTeams().length > 0;
    }

    private static String canonicalMatchId(Match match) {
        if (match.getId() != null) return match.getId().asMongoKey();
        return match.getMatchId();
    }
}