package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiCompletedMatchFanWorkProducer {
    public static final String HANDLER_KEY = "fan-match-comment";

    private final StageSourceRepository stageSources;
    private final AiCommunityMemberProfileRepository profiles;
    private final AiAutonomousWorkQueue queue;

    /**
     * Produces at most one fan-comment candidate per league system and finalized match.
     *
     * <p>The random initiative decision deliberately does not happen here. The durable
     * candidate must exist before the probability is sampled, otherwise repeated source
     * refreshes would re-roll a rejected decision until it eventually became true.</p>
     */
    public void onMatchFinalized(Match match) {
        if (!isEligibleFinalizedMatch(match)) return;

        String matchId = canonicalMatchId(match);
        List<AiCommunityMemberProfile> eligibleFans = eligibleFans(match);
        if (eligibleFans.isEmpty()) return;

        Set<String> leagueSystemIds = new LinkedHashSet<>();
        for (StageSource source : stageSources.findBySourceEntityId(match.getCompetitionId())) {
            if (source != null && StringUtils.hasText(source.getLeagueSystemId())) {
                leagueSystemIds.add(source.getLeagueSystemId().trim());
            }
        }

        for (String leagueSystemId : leagueSystemIds) {
            AiCommunityMemberProfile fan =
                    selectDeterministically(leagueSystemId, matchId, eligibleFans);

            queue.enqueue(new AiAutonomousWorkQueue.EnqueueRequest(
                    candidateKey(leagueSystemId, matchId),
                    HANDLER_KEY,
                    AiAutonomousWorkItem.WorkKind.FAN_MATCH_COMMENT,
                    AiAutonomousWorkItem.Priority.AUTONOMOUS,
                    leagueSystemId,
                    fan.getId(),
                    "MATCH",
                    matchId,
                    null,
                    1));
        }
    }

    static String candidateKey(String leagueSystemId, String matchId) {
        return "fan-match-comment:" + leagueSystemId + ":" + matchId;
    }

    static AiCommunityMemberProfile selectDeterministically(
            String leagueSystemId,
            String matchId,
            List<AiCommunityMemberProfile> eligibleFans) {
        int hash = (leagueSystemId + "|" + matchId).hashCode();
        int index = Math.floorMod(hash, eligibleFans.size());
        return eligibleFans.get(index);
    }

    private List<AiCommunityMemberProfile> eligibleFans(Match match) {
        return profiles.findByActiveTrueOrderByTeamIdAscOrdinalAsc().stream()
                .filter(fan -> fan != null
                        && StringUtils.hasText(fan.getId())
                        && fan.getUserId() != null
                        && StringUtils.hasText(fan.getUserSubject())
                        && StringUtils.hasText(fan.getTeamId()))
                .filter(fan -> teamParticipates(match, fan.getTeamId()))
                .toList();
    }

    private static boolean isEligibleFinalizedMatch(Match match) {
        return match != null
                && Boolean.TRUE.equals(match.getIsFinalized())
                && match.getCompetitionId() != null
                && StringUtils.hasText(canonicalMatchId(match))
                && match.getTeams() != null
                && match.getTeams().length > 0;
    }

    private static boolean teamParticipates(Match match, String teamId) {
        for (Team team : match.getTeams()) {
            if (team != null
                    && team.getId() != null
                    && teamId.equals(team.getId().asMongoKey())) {
                return true;
            }
        }
        return false;
    }

    private static String canonicalMatchId(Match match) {
        if (match == null) return null;
        if (match.getId() != null) return match.getId().asMongoKey();
        return match.getMatchId();
    }
}
