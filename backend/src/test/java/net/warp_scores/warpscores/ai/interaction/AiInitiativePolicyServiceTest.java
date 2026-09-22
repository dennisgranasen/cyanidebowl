package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.domain.persistence.AiLeagueSystemInitiativePolicyRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiInitiativeMode;
import net.warp_scores.warpscores.model.AiLeagueSystemInitiativePolicy;
import net.warp_scores.warpscores.model.AiSettings;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiInitiativePolicyServiceTest {
    private final AiSettingsRepository settings = mock(AiSettingsRepository.class);
    private final AiLeagueSystemInitiativePolicyRepository leagues =
            mock(AiLeagueSystemInitiativePolicyRepository.class);
    private final AiInitiativePolicyService service =
            new AiInitiativePolicyService(settings, leagues);

    @Test
    void codeDefaultsMatchProductPolicy() {
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.empty());

        assertThat(service.staffMode(null, AiInitiativePolicyService.StaffActivity.GENERAL_ARTICLE))
                .isEqualTo(AiInitiativeMode.REQUEST_ONLY);
        assertThat(service.staffMode(null, AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE))
                .isEqualTo(AiInitiativeMode.REQUEST_ONLY);
        assertThat(service.staffMode(null, AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT))
                .isEqualTo(AiInitiativeMode.AUTONOMOUS);
        assertThat(service.staffMode(null, AiInitiativePolicyService.StaffActivity.MATCH_COMMENT))
                .isEqualTo(AiInitiativeMode.AUTONOMOUS);
        assertThat(service.staffMode(null, AiInitiativePolicyService.StaffActivity.DIRECT_TAG_REPLY))
                .isEqualTo(AiInitiativeMode.AUTONOMOUS);
    }

    @Test
    void leagueOverrideWinsOverGlobal() {
        AiSettings global = new AiSettings();
        global.getInitiativePolicy().getStaff().setMatchArticles(AiInitiativeMode.DISABLED);
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.of(global));

        AiLeagueSystemInitiativePolicy override = new AiLeagueSystemInitiativePolicy();
        override.setLeagueSystemId("ls-1");
        override.getStaff().setMatchArticles(AiInitiativeMode.AUTONOMOUS);
        when(leagues.findById("ls-1")).thenReturn(Optional.of(override));

        assertThat(service.staffMode(
                "ls-1",
                AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE))
                .isEqualTo(AiInitiativeMode.AUTONOMOUS);
    }

    @Test
    void fanOwnTeamMatchRequiresActualTeamParticipation() {
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.empty());

        assertThat(service.fanShouldComment(
                "ls-1",
                new AiInitiativePolicyService.FanContext(
                        AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH,
                        false,
                        false),
                fixed(0.0))).isFalse();
    }

    @Test
    void fanOwnTeamMatchIsAutomaticByDefault() {
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.empty());

        assertThat(service.fanShouldComment(
                "ls-1",
                new AiInitiativePolicyService.FanContext(
                        AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH,
                        true,
                        false),
                fixed(0.99))).isTrue();
    }

    @Test
    void supportedTeamCoachActivityIsAutomaticByDefault() {
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.empty());

        assertThat(service.fanShouldComment(
                "ls-1",
                new AiInitiativePolicyService.FanContext(
                        AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH_ARTICLE,
                        true,
                        true),
                fixed(0.99))).isTrue();
    }

    private static RandomGenerator fixed(double value) {
        RandomGenerator rng = mock(RandomGenerator.class);
        when(rng.nextDouble()).thenReturn(value);
        return rng;
    }
}
