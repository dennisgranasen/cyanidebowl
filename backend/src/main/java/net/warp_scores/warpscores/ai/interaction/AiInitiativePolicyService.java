package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiLeagueSystemInitiativePolicyRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiInitiativeMode;
import net.warp_scores.warpscores.model.AiInitiativePolicy;
import net.warp_scores.warpscores.model.AiLeagueSystemInitiativePolicy;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.stereotype.Service;

import java.util.random.RandomGenerator;

@Service
@RequiredArgsConstructor
public class AiInitiativePolicyService {
    private final AiSettingsRepository settingsRepository;
    private final AiLeagueSystemInitiativePolicyRepository leaguePolicies;

    public enum StaffActivity {
        GENERAL_ARTICLE,
        MATCH_ARTICLE,
        ARTICLE_COMMENT,
        MATCH_COMMENT,
        DIRECT_TAG_REPLY
    }

    public enum FanTarget {
        GENERAL_ARTICLE,
        OWN_TEAM_ARTICLE,
        OWN_TEAM_MATCH_ARTICLE,
        OWN_TEAM_MATCH
    }

    public record EffectivePolicy(
            AiInitiativePolicy.Staff staff,
            AiInitiativePolicy.Fans fans) {
    }

    public record FanContext(
            FanTarget target,
            boolean supportedTeamParticipates,
            boolean authoredBySupportedTeamCoach) {
    }

    public EffectivePolicy effective(String leagueSystemId) {
        AiInitiativePolicy base = global();
        AiInitiativePolicy.Staff staff = copy(base.getStaff());
        AiInitiativePolicy.Fans fans = copy(base.getFans());

        if (leagueSystemId != null && !leagueSystemId.isBlank()) {
            leaguePolicies.findById(leagueSystemId)
                    .ifPresent(override -> {
                        apply(staff, override.getStaff());
                        apply(fans, override.getFans());
                    });
        }

        return new EffectivePolicy(staff, fans);
    }

    public AiInitiativeMode staffMode(
            String leagueSystemId,
            StaffActivity activity) {
        AiInitiativePolicy.Staff staff = effective(leagueSystemId).staff();
        return switch (activity) {
            case GENERAL_ARTICLE -> staff.getGeneralArticles();
            case MATCH_ARTICLE -> staff.getMatchArticles();
            case ARTICLE_COMMENT -> staff.getArticleComments();
            case MATCH_COMMENT -> staff.getMatchComments();
            case DIRECT_TAG_REPLY -> staff.getDirectTagReplies();
        };
    }

    public boolean staffMayRunAutonomously(
            String leagueSystemId,
            StaffActivity activity) {
        return staffMode(leagueSystemId, activity) == AiInitiativeMode.AUTONOMOUS;
    }

    public boolean staffMayRunWhenRequested(
            String leagueSystemId,
            StaffActivity activity) {
        return staffMode(leagueSystemId, activity) != AiInitiativeMode.DISABLED;
    }

    public boolean fanShouldComment(
            String leagueSystemId,
            FanContext context,
            RandomGenerator rng) {
        if (context == null || context.target() == null) return false;

        AiInitiativePolicy.Fans fans = effective(leagueSystemId).fans();

        if (context.authoredBySupportedTeamCoach()
                && context.supportedTeamParticipates()
                && fans.isOwnCoachActivityEnabled()) {
            return sample(fans.getOwnCoachActivityProbability(), rng);
        }

        return switch (context.target()) {
            case GENERAL_ARTICLE ->
                    fans.isGeneralArticleCommentsEnabled()
                            && sample(fans.getGeneralArticleCommentProbability(), rng);

            case OWN_TEAM_ARTICLE ->
                    context.supportedTeamParticipates()
                            && fans.isOwnTeamArticleCommentsEnabled()
                            && sample(fans.getOwnTeamArticleCommentProbability(), rng);

            case OWN_TEAM_MATCH_ARTICLE ->
                    context.supportedTeamParticipates()
                            && fans.isOwnTeamMatchArticleCommentsEnabled()
                            && sample(fans.getOwnTeamMatchArticleCommentProbability(), rng);

            case OWN_TEAM_MATCH ->
                    context.supportedTeamParticipates()
                            && fans.isOwnTeamMatchCommentsEnabled()
                            && sample(fans.getOwnTeamMatchCommentProbability(), rng);
        };
    }

    private AiInitiativePolicy global() {
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        return settings.getInitiativePolicy() == null
                ? new AiInitiativePolicy()
                : settings.getInitiativePolicy();
    }

    private static boolean sample(double probability, RandomGenerator rng) {
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;
        RandomGenerator actual = rng == null ? RandomGenerator.getDefault() : rng;
        return actual.nextDouble() < probability;
    }

    private static AiInitiativePolicy.Staff copy(AiInitiativePolicy.Staff source) {
        AiInitiativePolicy.Staff target = new AiInitiativePolicy.Staff();
        if (source == null) return target;
        target.setGeneralArticles(source.getGeneralArticles());
        target.setMatchArticles(source.getMatchArticles());
        target.setArticleComments(source.getArticleComments());
        target.setMatchComments(source.getMatchComments());
        target.setDirectTagReplies(source.getDirectTagReplies());
        return target;
    }

    private static AiInitiativePolicy.Fans copy(AiInitiativePolicy.Fans source) {
        AiInitiativePolicy.Fans target = new AiInitiativePolicy.Fans();
        if (source == null) return target;
        target.setGeneralArticleCommentsEnabled(source.isGeneralArticleCommentsEnabled());
        target.setGeneralArticleCommentProbability(source.getGeneralArticleCommentProbability());
        target.setOwnTeamArticleCommentsEnabled(source.isOwnTeamArticleCommentsEnabled());
        target.setOwnTeamArticleCommentProbability(source.getOwnTeamArticleCommentProbability());
        target.setOwnTeamMatchArticleCommentsEnabled(source.isOwnTeamMatchArticleCommentsEnabled());
        target.setOwnTeamMatchArticleCommentProbability(source.getOwnTeamMatchArticleCommentProbability());
        target.setOwnTeamMatchCommentsEnabled(source.isOwnTeamMatchCommentsEnabled());
        target.setOwnTeamMatchCommentProbability(source.getOwnTeamMatchCommentProbability());
        target.setOwnCoachActivityEnabled(source.isOwnCoachActivityEnabled());
        target.setOwnCoachActivityProbability(source.getOwnCoachActivityProbability());
        return target;
    }

    private static void apply(
            AiInitiativePolicy.Staff target,
            AiLeagueSystemInitiativePolicy.StaffOverride override) {
        if (override == null) return;
        if (override.getGeneralArticles() != null) target.setGeneralArticles(override.getGeneralArticles());
        if (override.getMatchArticles() != null) target.setMatchArticles(override.getMatchArticles());
        if (override.getArticleComments() != null) target.setArticleComments(override.getArticleComments());
        if (override.getMatchComments() != null) target.setMatchComments(override.getMatchComments());
        if (override.getDirectTagReplies() != null) target.setDirectTagReplies(override.getDirectTagReplies());
    }

    private static void apply(
            AiInitiativePolicy.Fans target,
            AiLeagueSystemInitiativePolicy.FanOverride override) {
        if (override == null) return;
        if (override.getGeneralArticleCommentsEnabled() != null) {
            target.setGeneralArticleCommentsEnabled(override.getGeneralArticleCommentsEnabled());
        }
        if (override.getGeneralArticleCommentProbability() != null) {
            target.setGeneralArticleCommentProbability(override.getGeneralArticleCommentProbability());
        }
        if (override.getOwnTeamArticleCommentsEnabled() != null) {
            target.setOwnTeamArticleCommentsEnabled(override.getOwnTeamArticleCommentsEnabled());
        }
        if (override.getOwnTeamArticleCommentProbability() != null) {
            target.setOwnTeamArticleCommentProbability(override.getOwnTeamArticleCommentProbability());
        }
        if (override.getOwnTeamMatchArticleCommentsEnabled() != null) {
            target.setOwnTeamMatchArticleCommentsEnabled(override.getOwnTeamMatchArticleCommentsEnabled());
        }
        if (override.getOwnTeamMatchArticleCommentProbability() != null) {
            target.setOwnTeamMatchArticleCommentProbability(override.getOwnTeamMatchArticleCommentProbability());
        }
        if (override.getOwnTeamMatchCommentsEnabled() != null) {
            target.setOwnTeamMatchCommentsEnabled(override.getOwnTeamMatchCommentsEnabled());
        }
        if (override.getOwnTeamMatchCommentProbability() != null) {
            target.setOwnTeamMatchCommentProbability(override.getOwnTeamMatchCommentProbability());
        }
        if (override.getOwnCoachActivityEnabled() != null) {
            target.setOwnCoachActivityEnabled(override.getOwnCoachActivityEnabled());
        }
        if (override.getOwnCoachActivityProbability() != null) {
            target.setOwnCoachActivityProbability(override.getOwnCoachActivityProbability());
        }
    }
}
